package ar.edu.itba.certiflow.infrastructure.assistance.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import ar.edu.itba.certiflow.assistance.model.AssistantReply;
import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewFactory;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewService;
import ar.edu.itba.certiflow.assistance.model.CriterionUnderReview;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ModelId;
import ar.edu.itba.certiflow.assistance.model.PromptVersion;
import ar.edu.itba.certiflow.assistance.model.ProposedFinding;
import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.infrastructure.inspection.persistence.JpaInspectionRepository;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.JpaInspectionSchemaRepository;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.infrastructure.support.PersistenceMappers;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Stamp;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaAssistantReviewRepository.class, JpaInspectionRepository.class, JpaInspectionSchemaRepository.class,
        PersistenceMappers.class})
class JpaAssistantReviewRepositoryTest {

    @Autowired
    private JpaAssistantReviewRepository repository;
    @Autowired
    private JpaInspectionRepository inspections;
    @Autowired
    private JpaInspectionSchemaRepository schemas;
    @Autowired
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    private final AssistantReviewService service = new AssistantReviewService(new AssistantReviewFactory(),
            mock(AuditLog.class));

    private InspectionSchema savedSchema() {
        InspectionSchema schema = DomainSamples.schema();
        schemas.add(schema);
        return schema;
    }

    private InspectionId savedInspection() {
        return savedInspection(savedSchema());
    }

    private InspectionId savedInspection(InspectionSchema schema) {
        Inspection inspection = DomainSamples.answeredInspection(schema, "7.00");
        inspections.save(inspection);
        return inspection.id();
    }

    private InspectionUnderReview underReview(InspectionId inspectionId) {
        return new InspectionUnderReview(inspectionId, DomainSamples.EXTINGUISHER, 1, false, List.of(
                new CriterionUnderReview(new CriterionCode("EXT-01"), "Gauge pressure within range", "numeric-range", "7.00",
                        Outcome.APPROVED, List.of("Needle trembles"), List.of()),
                new CriterionUnderReview(new CriterionCode("EXT-02"), "Seal intact", "yes-no", "true", Outcome.APPROVED,
                        List.of("Seal wire is cut"), List.of())));
    }

    private ProposedFinding proposal(String criterion, String quote) {
        return new ProposedFinding(new CriterionCode(criterion), StandardSeverity.CRITICAL, "Rationale for " + criterion,
                List.of(new Quote(quote)));
    }

    private AssistantReview reviewOf(InspectionId inspectionId, String id, int secondsAfterStart) {
        AssistantReply reply = new AssistantReply(new PromptVersion("v1"), new ModelId("claude-sonnet-5-5"), List.of(
                proposal("EXT-01", "needle trembles"), proposal("EXT-02", "wire is cut"), proposal("EXT-09", "wire")));
        return service.record(new ReviewId(id), underReview(inspectionId), reply,
                new Stamp(DomainSamples.INSPECTOR, DomainSamples.T0.plusSeconds(secondsAfterStart)));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void aReviewIsReadBackWithItsSuggestionsAndTheProposalsTheGuardrailsRejected() {
        AssistantReview review = reviewOf(savedInspection(), "R-1", 1000);
        repository.save(review);
        flushAndClear();

        AssistantReview read = repository.findById(review.id()).orElseThrow();

        assertEquals(review.snapshot(), read.snapshot());
        assertEquals(2, read.suggestions().size());
        assertEquals(1, read.rejectedProposals().size());
    }

    @Test
    void decisionsAreKeptAndPendingSuggestionsStayPending() {
        AssistantReview review = reviewOf(savedInspection(), "R-2", 1000);
        repository.save(review);
        flushAndClear();
        AssistantReview stored = repository.findById(review.id()).orElseThrow();
        service.dismiss(stored, 2, "The wire was cut on purpose for the recharge", new Stamp(DomainSamples.INSPECTOR,
                DomainSamples.T0.plusSeconds(2000)));
        repository.save(stored);
        flushAndClear();
        AssistantReview dismissed = repository.findById(review.id()).orElseThrow();
        service.accept(dismissed, 1, new Stamp(DomainSamples.INSPECTOR, DomainSamples.T0.plusSeconds(3000)));
        repository.save(dismissed);
        flushAndClear();

        AssistantReview read = repository.findById(review.id()).orElseThrow();

        assertEquals(dismissed.snapshot(), read.snapshot());
        assertTrue(read.suggestions().stream().noneMatch(suggestion -> suggestion.isPending()));
    }

    @Test
    void reviewsAreListedByInspectionInTheOrderTheyWereRequested() {
        InspectionSchema schema = savedSchema();
        InspectionId inspectionId = savedInspection(schema);
        repository.save(reviewOf(inspectionId, "R-later", 5000));
        repository.save(reviewOf(inspectionId, "R-earlier", 1000));
        repository.save(reviewOf(savedInspection(schema), "R-other", 3000));
        flushAndClear();

        assertEquals(List.of(new ReviewId("R-earlier"), new ReviewId("R-later")),
                repository.findByInspection(inspectionId).stream().map(AssistantReview::id).toList());
        assertEquals(inspectionId, repository.inspectionOf(new ReviewId("R-later")).orElseThrow());
        assertTrue(repository.inspectionOf(new ReviewId("R-missing")).isEmpty());
        assertNotEquals(repository.nextId(), repository.nextId());
    }

    @Test
    void legacyReviewsDoNotInventTheHistoricalInput() {
        AssistantReview review = reviewOf(savedInspection(), "R-legacy", 1000);
        repository.save(review);
        flushAndClear();
        entityManager.createNativeQuery("update assistant_review set reviewed_input = null where id = :id")
                .setParameter("id", review.id().value()).executeUpdate();
        entityManager.clear();
        assertEquals(new ar.edu.itba.certiflow.assistance.model.UnavailableReviewInput(),
                repository.findById(review.id()).orElseThrow().reviewedInput());
    }

}
