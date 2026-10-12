package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepository;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.NumericAnswer;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaInspectionRepository.class, JpaInspectionSchemaRepository.class})
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
        schemas.save(schema);
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
                .recordAnswer(restored, "EXT-02", new RawAnswer.YesNo(false), DomainSamples.INSPECTOR, DomainSamples.T0));
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
}
