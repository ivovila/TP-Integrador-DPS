package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.infrastructure.support.PersistenceMappers;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaAlreadyDefinedForAssetTypeException;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Page;
import ar.edu.itba.certiflow.shared.model.PageRequest;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import ar.edu.itba.certiflow.shared.model.Stamp;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaInspectionSchemaRepository.class, PersistenceMappers.class})
class JpaInspectionSchemaRepositoryTest {


    @Autowired
    private JpaInspectionSchemaRepository repository;
    @Autowired
    private StoredSchemaVersions storedSchemaVersions;
    @Autowired
    private EntityManager entityManager;

    @Test
    void everyVersionIsReadBackEqualToWhatWasPublished() {
        AssetType extinguisher = new AssetType("Fire extinguisher");
        InspectionSchema schema = DomainSamples.schemaWith("Extinguisher", extinguisher, DomainSamples.sections(), Instant.parse("2026-03-01T00:00:00Z"))
                .withNewVersion(DomainSamples.sections().subList(0, 1), Instant.parse("2026-03-05T00:00:00Z"), Instant.parse("2026-03-05T00:00:00Z"), Set.of());

        repository.add(schema);
        entityManager.flush();
        entityManager.clear();

        assertEquals(schema, repository.findByAssetType(extinguisher).orElseThrow());
    }

    @Test
    void savingAgainAppendsTheNewVersionWithoutTouchingThePreviousOnes() {
        AssetType extinguisher = new AssetType("Fire extinguisher");
        InspectionSchema first = DomainSamples.schemaWith("Extinguisher", extinguisher, DomainSamples.sections(), Instant.parse("2026-03-01T00:00:00Z"));
        repository.add(first);
        entityManager.flush();
        Object firstVersionId = rowIdOf(storedSchemaVersions.entityOf(extinguisher, 1));

        repository.save(first.withNewVersion(DomainSamples.sections(), Instant.parse("2026-03-05T00:00:00Z"), Instant.parse("2026-03-05T00:00:00Z"), Set.of()));
        entityManager.flush();
        entityManager.clear();

        assertEquals(firstVersionId, rowIdOf(storedSchemaVersions.entityOf(extinguisher, 1)));
        assertEquals(2, repository.findByAssetType(extinguisher).orElseThrow().versions().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void aCancelledVersionIsReadBackCancelledAndAnotherOneCanTakeItsDate() {
        AssetType extinguisher = new AssetType("Fire extinguisher");
        Instant pendingFrom = Instant.parse("2026-04-01T00:00:00Z");
        InspectionSchema pending = DomainSamples.schemaWith("Extinguisher", extinguisher, DomainSamples.sections(), Instant.parse("2026-03-01T00:00:00Z"))
                .withNewVersion(DomainSamples.sections(), Instant.parse("2026-03-05T00:00:00Z"), pendingFrom, Set.of());
        repository.add(pending);
        entityManager.flush();
        InspectionSchemaService service = new InspectionSchemaService(mock(AuditLog.class));
        InspectionSchema cancelled = service.cancelVersion(pending, 2,
                new Stamp(new Person(new PersonId("EMP-006"), "Ada Admin"), Instant.parse("2026-03-06T00:00:00Z")), "Wrong severity");

        repository.save(cancelled.withNewVersion(DomainSamples.sections(), Instant.parse("2026-03-07T00:00:00Z"), pendingFrom, Set.of()));
        entityManager.flush();
        entityManager.clear();

        InspectionSchema stored = repository.findByAssetType(extinguisher).orElseThrow();
        assertEquals(cancelled.version(2), stored.version(2));
        assertEquals(3, stored.versionAt(pendingFrom).number());
    }

    private Object rowIdOf(SchemaVersionEntity versionEntity) {
        return entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(versionEntity);
    }

    /** Crear un esquema es darlo de alta: si el tipo de activo ya tiene uno falla, no lo pisa ni se pierde en silencio. */
    @Test
    void aSecondSchemaForAnAssetTypeThatAlreadyHasOneIsRejectedAndTheFirstOneIsKept() {
        AssetType extinguisher = new AssetType("Fire extinguisher");
        repository.add(DomainSamples.schemaWith("Extinguisher", extinguisher, DomainSamples.sections(),
                Instant.parse("2026-03-01T00:00:00Z")));
        entityManager.flush();
        entityManager.clear();

        assertThrows(SchemaAlreadyDefinedForAssetTypeException.class, () -> repository.add(DomainSamples.schemaWith(
                "Another schema", extinguisher, DomainSamples.sections().subList(0, 1), Instant.parse("2026-03-02T00:00:00Z"))));

        entityManager.clear();
        InspectionSchema stored = repository.findByAssetType(extinguisher).orElseThrow();
        assertEquals("Extinguisher", stored.name());
        assertEquals(1, stored.versions().size());
    }

    @Test
    void schemasAreListedByPageOrderedByAssetType() {
        Instant createdAt = Instant.parse("2026-03-01T00:00:00Z");
        repository.add(DomainSamples.schemaWith("Extinguisher", new AssetType("Fire extinguisher"), DomainSamples.sections(), createdAt));
        repository.add(DomainSamples.schemaWith("Boiler", new AssetType("Boiler"), DomainSamples.sections(), createdAt));
        entityManager.flush();
        entityManager.clear();
        Page<InspectionSchema> first = repository.findPage(new PageRequest(0, 1));
        Page<InspectionSchema> second = repository.findPage(new PageRequest(1, 1));
        assertEquals(List.of("Boiler"), first.items().stream().map(schema -> schema.assetType().name()).toList());
        assertEquals(List.of("Fire extinguisher"), second.items().stream().map(schema -> schema.assetType().name()).toList());
        assertEquals(2, first.totalItems());
        assertEquals(2, first.totalPages());
    }
}
