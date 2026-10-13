package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.OptionInListRule;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.Unit;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import ar.edu.itba.certiflow.shared.model.Page;
import ar.edu.itba.certiflow.shared.model.PageRequest;

@DataJpaTest
@Import(JpaInspectionSchemaRepository.class)
class JpaInspectionSchemaRepositoryTest {


    @Autowired
    private JpaInspectionSchemaRepository repository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void everyVersionIsReadBackEqualToWhatWasPublished() {
        AssetType extinguisher = new AssetType("Fire extinguisher");
        InspectionSchema schema = new InspectionSchema("Extinguisher", extinguisher, DomainSamples.sections(), Instant.parse("2026-03-01T00:00:00Z"))
                .withNewVersion(DomainSamples.sections().subList(0, 1), Instant.parse("2026-03-05T00:00:00Z"), Instant.parse("2026-03-05T00:00:00Z"), Set.of());

        repository.save(schema);
        entityManager.flush();
        entityManager.clear();

        assertEquals(schema, repository.findByAssetType(extinguisher).orElseThrow());
    }

    @Test
    void savingAgainAppendsTheNewVersionWithoutTouchingThePreviousOnes() {
        AssetType extinguisher = new AssetType("Fire extinguisher");
        InspectionSchema first = new InspectionSchema("Extinguisher", extinguisher, DomainSamples.sections(), Instant.parse("2026-03-01T00:00:00Z"));
        repository.save(first);
        entityManager.flush();
        Long firstVersionId = repository.versionEntity(extinguisher, 1).getId();

        repository.save(first.withNewVersion(DomainSamples.sections(), Instant.parse("2026-03-05T00:00:00Z"), Instant.parse("2026-03-05T00:00:00Z"), Set.of()));
        entityManager.flush();
        entityManager.clear();

        assertEquals(firstVersionId, repository.versionEntity(extinguisher, 1).getId());
        assertEquals(2, repository.findByAssetType(extinguisher).orElseThrow().versions().size());
    }

    @Test
    void schemasAreListedByPageOrderedByAssetType() {
        Instant createdAt = Instant.parse("2026-03-01T00:00:00Z");
        repository.save(new InspectionSchema("Extinguisher", new AssetType("Fire extinguisher"), DomainSamples.sections(), createdAt));
        repository.save(new InspectionSchema("Boiler", new AssetType("Boiler"), DomainSamples.sections(), createdAt));
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
