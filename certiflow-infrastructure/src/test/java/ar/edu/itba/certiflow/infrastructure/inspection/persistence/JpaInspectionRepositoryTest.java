package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepository;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.infrastructure.support.PersistenceMappers;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.SectionsScope;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.NumericAnswer;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.SectionTitle;
import ar.edu.itba.certiflow.shared.model.Stamp;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaInspectionRepository.class, JpaInspectionSchemaRepository.class, PersistenceMappers.class})
class JpaInspectionRepositoryTest {

    @Autowired
    private JpaInspectionRepository repository;
    @Autowired
    private JpaInspectionSchemaRepository schemas;
    @Autowired
    private EntityManager entityManager;
    private InspectionSchema schema;

    @BeforeEach
    void publishSchema() {
        schema = DomainSamples.schema();
        schemas.add(schema);
    }

    private Inspection reloaded(Inspection inspection) {
        repository.save(inspection);
        entityManager.flush();
        entityManager.clear();
        return repository.findById(inspection.id()).orElseThrow();
    }

    @Test
    void anOpenInspectionIsReadBackWithTheSameSnapshot() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");

        assertEquals(inspection.snapshot(), reloaded(inspection).snapshot());
    }

    @Test
    void aClosedInspectionIsReadBackClosedAndStillRejectsAnswers() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");
        DomainSamples.close(inspection);

        Inspection restored = reloaded(inspection);

        assertEquals(inspection.snapshot(), restored.snapshot());
        assertTrue(restored.isClosed());
        assertThrows(InspectionAlreadyClosedException.class, () -> DomainSamples.inspectionService()
                .recordAnswer(restored, new CriterionCode("EXT-02"), new RawAnswer("false"), new Stamp(DomainSamples.INSPECTOR, DomainSamples.T0)));
    }

    @Test
    void decimalScaleSurvivesTheDatabase() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");

        NumericAnswer answer = (NumericAnswer) reloaded(inspection).evaluate().responses().getFirst().given().orElseThrow().value();

        assertEquals("7.00", answer.value().toPlainString());
    }

    @Test
    void savingTwiceUpdatesInsteadOfDuplicating() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");
        repository.save(inspection);
        entityManager.flush();
        DomainSamples.close(inspection);

        assertEquals(inspection.snapshot(), reloaded(inspection).snapshot());
    }

    @Test
    void aSectionTitleWithALineBreakSurvivesTheScope() {
        InspectionSchema lined = DomainSamples.schemaWith("Lined", new AssetType("Lined boiler"),
                List.of(new Section(new SectionTitle("Signage\nand lights"), DomainSamples.sections().get(1).criteria())), DomainSamples.T0);
        schemas.add(lined);
        Asset sample = DomainSamples.ASSET;
        Asset linedBoiler = new Asset(sample.code(), sample.name(), lined.assetType(), sample.location(), sample.responsible(),
                sample.characteristics(), sample.jurisdictionCode(), sample.subsystems());
        Inspection inspection = DomainSamples.inspectionService().assign(new InspectionId("I-lined"), linedBoiler,
                new InspectionAssignment(DomainSamples.INSPECTOR, LocalDate.of(2026, 3, 13), new SectionsScope(Set.of(new SectionTitle("Signage\nand lights")))),
                lined.latestPublishedVersion(), new Stamp(DomainSamples.PLANNER, DomainSamples.T0));

        assertEquals(inspection.snapshot(), reloaded(inspection).snapshot());
    }

    /** El reloj del sistema entrega microsegundos, que es lo que guardan tanto la columna como el documento. */
    @Test
    void aRevisionKeepsTheSamePrecisionAsTheAnswerItSealed() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");
        DomainSamples.inspectionService().recordAnswer(inspection, new CriterionCode("EXT-02"), new RawAnswer("true"), new Stamp(DomainSamples.INSPECTOR, DomainSamples.T0.plusNanos(123_456_000)));
        DomainSamples.close(inspection);

        Inspection restored = reloaded(inspection);

        assertEquals(DomainSamples.T0.plusNanos(123_456_000),
                restored.evaluate().responses().get(1).given().orElseThrow().answeredAt());
        assertEquals(restored.evaluate().responses().get(1).given().orElseThrow().answeredAt(),
                restored.originalRevision().evaluation().responses().get(1).given().orElseThrow().answeredAt());
    }

    @Test
    void recordingOneAnswerDoesNotRewriteTheEvidenceAndObservations() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");
        repository.save(inspection);
        entityManager.flush();
        entityManager.clear();
        Inspection loaded = repository.findById(inspection.id()).orElseThrow();
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        DomainSamples.inspectionService().recordAnswer(loaded, new CriterionCode("EXT-02"), new RawAnswer("false"), new Stamp(DomainSamples.INSPECTOR, DomainSamples.T0.plusSeconds(900)));
        repository.save(loaded);
        entityManager.flush();

        assertEquals(0, statistics.getCollectionRecreateCount() + statistics.getCollectionRemoveCount());
    }

    @Test
    void rectifyingDoesNotRewriteTheRevisionsAlreadySealed() {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");
        DomainSamples.close(inspection);
        repository.save(inspection);
        entityManager.flush();
        entityManager.clear();
        Inspection loaded = repository.findById(inspection.id()).orElseThrow();
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        DomainSamples.inspectionService().rectify(loaded, new CriterionCode("EXT-03"), new RawAnswer("REFLECTIVE"), "Typo", new Stamp(DomainSamples.PLANNER, DomainSamples.T0.plusSeconds(1200)));
        repository.save(loaded);
        entityManager.flush();

        assertEquals(0, statistics.getCollectionRecreateCount() + statistics.getCollectionRemoveCount());
    }
}
