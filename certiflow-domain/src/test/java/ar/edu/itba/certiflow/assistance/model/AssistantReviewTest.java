package ar.edu.itba.certiflow.assistance.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.assistance.model.exceptions.DismissalReasonRequiredException;
import ar.edu.itba.certiflow.assistance.model.exceptions.ReviewOfClosedInspectionException;
import ar.edu.itba.certiflow.assistance.model.exceptions.SuggestionAlreadyDecidedException;
import ar.edu.itba.certiflow.assistance.model.exceptions.SuggestionNotFoundException;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AssistantReviewTest {

    private final Person inspector = new Person(new PersonId("EMP-002"), "Ines Inspector");
    private final Instant requestedAt = Instant.parse("2026-11-07T12:00:00Z");
    private final Stamp requested = new Stamp(inspector, requestedAt);
    private final Stamp decided = new Stamp(inspector, requestedAt.plusSeconds(60));
    private final ReviewId reviewId = new ReviewId("REV-1");
    private final InspectionId inspectionId = new InspectionId("INS-1");
    private final RecordingAuditLog auditLog = new RecordingAuditLog();
    private final AssistantReviewFactory factory = new AssistantReviewFactory();
    private final AssistantReviewService service = new AssistantReviewService(factory, auditLog);

    private final CriterionUnderReview hose = new CriterionUnderReview(new CriterionCode("HOSE"), "Hose condition",
            "one of Good, Worn", "Good", Outcome.APPROVED,
            List.of("La manguera tiene una grieta   cerca del acople"), List.of());
    private final CriterionUnderReview pressure = new CriterionUnderReview(new CriterionCode("PRESSURE"), "Pressure",
            "between 10 and 15", "8.0", Outcome.REJECTED, List.of(), List.of());
    private final CriterionUnderReview height = new CriterionUnderReview(new CriterionCode("HEIGHT"), "Mounting height",
            "between 1.0 and 1.5", "1.2", Outcome.APPROVED, List.of(),
            List.of(new EvidenceUnderReview("PHOTO", "Una estantería tapa el cartel de señalización")));
    private final CriterionUnderReview signage = new CriterionUnderReview(new CriterionCode("SIGNAGE"), "Signage visible",
            "yes", "true", Outcome.APPROVED, List.of(), List.of());

    private InspectionUnderReview openInspection() {
        return new InspectionUnderReview(inspectionId, new AssetType("Fire extinguisher"), 1, false,
                List.of(hose, pressure, height, signage));
    }

    private ProposedFinding proposal(String criterion, String... quotes) {
        return new ProposedFinding(new CriterionCode(criterion), StandardSeverity.MINOR, "The text describes a problem",
                List.of(quotes).stream().map(Quote::new).toList());
    }

    private AssistantReply reply(ProposedFinding... proposals) {
        return new AssistantReply(new PromptVersion("v1"), new ModelId("stub"), List.of(proposals));
    }

    private AssistantReview reviewOf(ProposedFinding... proposals) {
        return service.record(reviewId, openInspection(), reply(proposals), requested);
    }

    @Test
    void aProposalBackedByWhatTheInspectorWroteBecomesAPendingSuggestion() {
        AssistantReview review = reviewOf(proposal("HOSE", "grieta cerca del acople"));

        assertEquals(1, review.suggestions().size());
        assertTrue(review.suggestion(1).isPending());
        assertEquals(new CriterionCode("HOSE"), review.suggestion(1).proposal().criterion());
        assertTrue(review.rejectedProposals().isEmpty());
    }

    @Test
    void aQuoteMatchesIgnoringCaseAndSpacing() {
        AssistantReview review = reviewOf(proposal("HOSE", "  GRIETA cerca\ndel acople "));

        assertEquals(1, review.suggestions().size());
    }

    @Test
    void aQuoteMayComeFromAnotherCriterion() {
        AssistantReview review = reviewOf(proposal("SIGNAGE", "una estantería tapa el cartel"));

        assertEquals(1, review.suggestions().size());
    }

    @Test
    void aCriterionOutsideTheInspectionIsRejectedAsOutOfScope() {
        AssistantReview review = reviewOf(proposal("WEIGHT", "grieta"));

        assertRejectedAs(GuardrailRejection.OUT_OF_SCOPE, review);
    }

    @Test
    void aCriterionTheRuleAlreadyRejectsIsNotSuggestedAgain() {
        AssistantReview review = reviewOf(proposal("PRESSURE", "grieta"));

        assertRejectedAs(GuardrailRejection.ALREADY_NONCONFORMING, review);
    }

    @ParameterizedTest
    @ValueSource(strings = {"la manguera está rota", "grieta cerca de la válvula"})
    void aQuoteTheInspectorNeverWroteIsRejected(String invented) {
        AssistantReview review = reviewOf(proposal("HOSE", "grieta", invented));

        assertRejectedAs(GuardrailRejection.UNSUPPORTED_QUOTE, review);
    }

    @Test
    void aProposalWithoutQuotesIsRejected() {
        AssistantReview review = reviewOf(proposal("HOSE"));

        assertRejectedAs(GuardrailRejection.UNSUPPORTED_QUOTE, review);
    }

    @Test
    void theFirstGuardrailThatRejectsIsTheRecordedReason() {
        AssistantReview review = reviewOf(proposal("WEIGHT"));

        assertRejectedAs(GuardrailRejection.OUT_OF_SCOPE, review);
    }

    @Test
    void suggestionsAreNumberedFromOneSkippingTheRejectedOnes() {
        AssistantReview review = reviewOf(proposal("PRESSURE", "grieta"), proposal("HOSE", "grieta"),
                proposal("SIGNAGE", "estantería"));

        assertEquals(List.of(1, 2), review.suggestions().stream().map(Suggestion::position).toList());
        assertEquals(new CriterionCode("SIGNAGE"), review.suggestion(2).proposal().criterion());
        assertEquals(1, review.rejectedProposals().size());
    }

    @Test
    void aClosedInspectionCannotBeReviewed() {
        InspectionUnderReview closed = new InspectionUnderReview(inspectionId, new AssetType("Fire extinguisher"), 1, true,
                List.of(hose));

        assertThrows(ReviewOfClosedInspectionException.class,
                () -> service.record(reviewId, closed, reply(proposal("HOSE", "grieta")), requested));
        assertTrue(auditLog.entries.isEmpty());
    }

    @Test
    void requestingAReviewIsAuditedWithWhatTheModelAndTheGuardrailsDid() {
        reviewOf(proposal("HOSE", "grieta"), proposal("PRESSURE", "grieta"));

        assertEquals(1, auditLog.entries.size());
        AuditEntry entry = auditLog.entries.getFirst();
        assertEquals(AssistanceAudit.REVIEW_REQUESTED, entry.action());
        assertEquals(requested, entry.performed());
        assertEquals("1 suggestions, 1 rejected by guardrails, prompt v1, model stub", entry.detail());
    }

    @Test
    void acceptingASuggestionOnlyRecordsTheDecision() {
        AssistantReview review = reviewOf(proposal("HOSE", "grieta"));

        service.accept(review, 1, decided);

        assertEquals(new Acceptance(decided), review.suggestion(1).decision());
        assertFalse(review.suggestion(1).isPending());
        assertEquals(AssistanceAudit.SUGGESTION_ACCEPTED, auditLog.entries.getLast().action());
        assertEquals("Suggestion 1 on HOSE", auditLog.entries.getLast().detail());
    }

    @Test
    void dismissingASuggestionKeepsTheReason() {
        AssistantReview review = reviewOf(proposal("HOSE", "grieta"));

        service.dismiss(review, 1, "The crack is on the bracket, not the hose", decided);

        assertEquals(new Dismissal(decided, "The crack is on the bracket, not the hose"), review.suggestion(1).decision());
        assertEquals(AssistanceAudit.SUGGESTION_DISMISSED, auditLog.entries.getLast().action());
        assertEquals("Suggestion 1 on HOSE: The crack is on the bracket, not the hose", auditLog.entries.getLast().detail());
    }

    @Test
    void dismissingWithoutAReasonIsRejected() {
        AssistantReview review = reviewOf(proposal("HOSE", "grieta"));

        assertThrows(DismissalReasonRequiredException.class, () -> service.dismiss(review, 1, " ", decided));
        assertTrue(review.suggestion(1).isPending());
    }

    @Test
    void aDecidedSuggestionCannotBeDecidedAgain() {
        AssistantReview review = reviewOf(proposal("HOSE", "grieta"));
        service.accept(review, 1, decided);

        assertThrows(SuggestionAlreadyDecidedException.class, () -> service.accept(review, 1, decided));
        assertThrows(SuggestionAlreadyDecidedException.class, () -> service.dismiss(review, 1, "Changed my mind", decided));
    }

    @Test
    void anUnknownPositionIsNotFound() {
        AssistantReview review = reviewOf(proposal("HOSE", "grieta"));

        assertThrows(SuggestionNotFoundException.class, () -> service.accept(review, 2, decided));
    }

    @Test
    void aRestoredReviewKeepsItsDecisionsWithoutAuditingAgain() {
        AssistantReview review = reviewOf(proposal("HOSE", "grieta"), proposal("SIGNAGE", "estantería"),
                proposal("WEIGHT", "grieta"));
        service.dismiss(review, 2, "Not a signage problem", decided);
        int auditedBefore = auditLog.entries.size();

        AssistantReview restored = factory.restore(review.snapshot());

        assertEquals(review.snapshot(), restored.snapshot());
        assertTrue(restored.suggestion(1).isPending());
        assertFalse(restored.suggestion(2).isPending());
        assertEquals(GuardrailRejection.OUT_OF_SCOPE, restored.rejectedProposals().getFirst().reason());
        assertTrue(restored.pullAuditEntries().isEmpty());
        assertEquals(auditedBefore, auditLog.entries.size());
    }

    private void assertRejectedAs(GuardrailRejection expected, AssistantReview review) {
        assertTrue(review.suggestions().isEmpty());
        assertEquals(1, review.rejectedProposals().size());
        assertEquals(expected, review.rejectedProposals().getFirst().reason());
    }

    private static final class RecordingAuditLog implements AuditLog<ReviewId> {

        private final List<AuditEntry> entries = new ArrayList<>();

        @Override
        public void record(ReviewId subject, AuditEntry entry) {
            entries.add(entry);
        }

        @Override
        public List<AuditEntry> historyOf(ReviewId subject) {
            return List.copyOf(entries);
        }
    }
}
