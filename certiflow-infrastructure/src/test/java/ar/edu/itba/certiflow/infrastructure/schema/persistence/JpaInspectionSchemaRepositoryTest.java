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
                .withNewVersion(DomainSamples.sections().subList(0, 1), Instant.parse("2026-03-05T00:00:00Z"));

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

        repository.save(first.withNewVersion(DomainSamples.sections(), Instant.parse("2026-03-05T00:00:00Z")));
        entityManager.flush();
        entityManager.clear();

        assertEquals(firstVersionId, repository.versionEntity(extinguisher, 1).getId());
        assertEquals(2, repository.findByAssetType(extinguisher).orElseThrow().versions().size());
    }
}
