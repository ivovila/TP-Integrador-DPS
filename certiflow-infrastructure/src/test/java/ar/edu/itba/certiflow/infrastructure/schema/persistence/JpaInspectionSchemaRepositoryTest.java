package ar.edu.itba.certiflow.infrastructure.schema.persistence;

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

    static List<Section> sections() {
        return List.of(
                new Section("Pressure and seal", List.of(
                        new Criterion<>("EXT-01", "Gauge pressure within range",
                                new NumericRangeRule(new Unit("bar"), new Range(new BigDecimal("6.00"), new BigDecimal("8.00"))),
                                StandardSeverity.MAJOR, List.of(new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1))),
                        new Criterion<>("EXT-02", "Seal intact", new YesNoRule(YesNoAnswer.YES), StandardSeverity.CRITICAL,
                                List.of()))),
                new Section("Signage", List.of(new Criterion<>("EXT-03", "Signage condition",
                        new OptionInListRule(Set.of("VISIBLE", "REFLECTIVE")), StandardSeverity.MINOR, List.of()))));
    }

    @Autowired
    private JpaInspectionSchemaRepository repository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void everyVersionIsReadBackEqualToWhatWasPublished() {
        AssetType extinguisher = new AssetType("Fire extinguisher");
        InspectionSchema schema = new InspectionSchema("Extinguisher", extinguisher, sections(), Instant.parse("2026-03-01T00:00:00Z"))
                .withNewVersion(sections().subList(0, 1), Instant.parse("2026-03-05T00:00:00Z"));

        repository.save(schema);
        entityManager.flush();
        entityManager.clear();

        assertEquals(schema, repository.findByAssetType(extinguisher).orElseThrow());
    }

    @Test
    void savingAgainAppendsTheNewVersionWithoutTouchingThePreviousOnes() {
        AssetType extinguisher = new AssetType("Fire extinguisher");
        InspectionSchema first = new InspectionSchema("Extinguisher", extinguisher, sections(), Instant.parse("2026-03-01T00:00:00Z"));
        repository.save(first);
        entityManager.flush();
        Long firstVersionId = repository.versionEntity(extinguisher, 1).getId();

        repository.save(first.withNewVersion(sections(), Instant.parse("2026-03-05T00:00:00Z")));
        entityManager.flush();
        entityManager.clear();

        assertEquals(firstVersionId, repository.versionEntity(extinguisher, 1).getId());
        assertEquals(2, repository.findByAssetType(extinguisher).orElseThrow().versions().size());
    }
}
