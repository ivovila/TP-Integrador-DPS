package ar.edu.itba.certiflow.infrastructure.assistance.persistence;

import ar.edu.itba.certiflow.assistance.model.AssistantReviewSnapshot;
import ar.edu.itba.certiflow.assistance.model.ModelId;
import ar.edu.itba.certiflow.assistance.model.PendingDecision;
import ar.edu.itba.certiflow.assistance.model.PromptVersion;
import ar.edu.itba.certiflow.assistance.model.ProposedFinding;
import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.assistance.model.Suggestion;
import ar.edu.itba.certiflow.assistance.model.SuggestionDecision;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Stamp;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Entity
@Table(name = "assistant_review")
public class AssistantReviewEntity {

    @Id
    private String id;
    @Column(name = "inspection_id")
    private String inspectionId;
    @Column(name = "prompt_version")
    private String promptVersion;
    @Column(name = "model_id")
    private String modelId;
    @Column(name = "requested_by_id")
    private String requestedById;
    @Column(name = "requested_by_name")
    private String requestedByName;
    @Column(name = "requested_at")
    private Instant requestedAt;
    @Column(name = "reviewed_input")
    private String reviewedInput;
    private String suggestions;
    @Column(name = "rejected_proposals")
    private String rejectedProposals;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "suggestion_decision", joinColumns = @JoinColumn(name = "review_id"))
    private List<SuggestionDecisionEmbeddable> decisions = new ArrayList<>();

    protected AssistantReviewEntity() {
    }

    AssistantReviewEntity(AssistantReviewSnapshot snapshot, ProposalsDocumentMapper proposalsDocuments) {
        PersonColumns requestedBy = new PersonColumns(snapshot.requested().author());
        this.id = snapshot.id().value();
        this.inspectionId = snapshot.inspectionId().value();
        this.promptVersion = snapshot.promptVersion().value();
        this.modelId = snapshot.modelId().value();
        this.requestedById = requestedBy.id();
        this.requestedByName = requestedBy.name();
        this.requestedAt = snapshot.requested().instant();
        this.reviewedInput = proposalsDocuments.writeInput(snapshot.reviewedInput());
        this.suggestions = proposalsDocuments.writeProposals(snapshot.suggestions().stream().map(Suggestion::proposal).toList());
        this.rejectedProposals = proposalsDocuments.writeRejected(snapshot.rejectedProposals());
        copyDecisionsFrom(snapshot);
    }

    /** Las sugerencias y las rechazadas no cambian: lo único que se actualiza son las decisiones. */
    void copyDecisionsFrom(AssistantReviewSnapshot snapshot) {
        decisions.clear();
        snapshot.suggestions().stream()
                .flatMap(suggestion -> suggestion.decision().describedBy(new StoredDecisionDescriber(suggestion.position())))
                .forEach(decisions::add);
    }

    AssistantReviewSnapshot toSnapshot(ProposalsDocumentMapper proposalsDocuments) {
        List<ProposedFinding> proposals = proposalsDocuments.readProposals(suggestions);
        return new AssistantReviewSnapshot(new ReviewId(id), new InspectionId(inspectionId), new PromptVersion(promptVersion),
                new ModelId(modelId), new Stamp(new PersonColumns(requestedById, requestedByName).toDomain(), requestedAt),
                IntStream.range(0, proposals.size())
                        .mapToObj(index -> new Suggestion(index + 1, proposals.get(index), decisionAt(index + 1)))
                        .toList(),
                proposalsDocuments.readRejected(rejectedProposals), proposalsDocuments.readInput(reviewedInput));
    }

    private SuggestionDecision decisionAt(int position) {
        return decisions.stream()
                .filter(decision -> decision.isFor(position))
                .findFirst()
                .map(SuggestionDecisionEmbeddable::toDomain)
                .orElseGet(PendingDecision::new);
    }
}
