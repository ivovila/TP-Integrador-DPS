package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.assistance.model.exceptions.SuggestionNotFoundException;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditTrail;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Lo que sugirió el asistente sobre una inspección abierta y lo que decidió el inspector. Nunca toca la inspección:
 * aceptar o descartar solo deja la decisión trazada.
 */
public final class AssistantReview {

    private final ReviewId id;
    private final InspectionId inspectionId;
    private final PromptVersion promptVersion;
    private final ModelId modelId;
    private final Stamp requested;
    private final List<Suggestion> suggestions;
    private final List<RejectedProposal> rejectedProposals;
    private final AuditTrail auditTrail = new AuditTrail();

    AssistantReview(ReviewId id, InspectionUnderReview inspection, AssistantReply reply, Stamp requested) {
        inspection.ensureOpen();
        this.id = id;
        this.inspectionId = inspection.inspectionId();
        this.promptVersion = reply.promptVersion();
        this.modelId = reply.modelId();
        this.requested = requested;
        this.rejectedProposals = reply.proposals().stream()
                .flatMap(proposal -> firstRejectionOf(proposal, inspection)
                        .map(reason -> new RejectedProposal(proposal, reason))
                        .stream())
                .toList();
        this.suggestions = numbered(reply.proposals().stream()
                .filter(proposal -> firstRejectionOf(proposal, inspection).isEmpty())
                .toList());
        auditTrail.record(AssistanceAudit.REVIEW_REQUESTED, requested, suggestions.size() + " suggestions, "
                + rejectedProposals.size() + " rejected by guardrails, prompt " + promptVersion.value()
                + ", model " + modelId.value());
    }

    AssistantReview(AssistantReviewSnapshot snapshot) {
        this.id = snapshot.id();
        this.inspectionId = snapshot.inspectionId();
        this.promptVersion = snapshot.promptVersion();
        this.modelId = snapshot.modelId();
        this.requested = snapshot.requested();
        this.suggestions = new ArrayList<>(snapshot.suggestions());
        this.rejectedProposals = snapshot.rejectedProposals();
    }

    public AssistantReviewSnapshot snapshot() {
        return new AssistantReviewSnapshot(id, inspectionId, promptVersion, modelId, requested, suggestions, rejectedProposals);
    }

    void accept(int position, Stamp accepted) {
        Suggestion acceptedSuggestion = suggestion(position).accepted(accepted);
        replace(acceptedSuggestion);
        auditTrail.record(AssistanceAudit.SUGGESTION_ACCEPTED, accepted, describe(acceptedSuggestion));
    }

    void dismiss(int position, String reason, Stamp dismissed) {
        Suggestion dismissedSuggestion = suggestion(position).dismissed(reason, dismissed);
        replace(dismissedSuggestion);
        auditTrail.record(AssistanceAudit.SUGGESTION_DISMISSED, dismissed, describe(dismissedSuggestion) + ": " + reason);
    }

    public Suggestion suggestion(int position) {
        return suggestions.stream()
                .filter(suggestion -> suggestion.position() == position)
                .findFirst()
                .orElseThrow(() -> new SuggestionNotFoundException(id, position));
    }

    public ReviewId id() {
        return id;
    }

    public InspectionId inspectionId() {
        return inspectionId;
    }

    public PromptVersion promptVersion() {
        return promptVersion;
    }

    public ModelId modelId() {
        return modelId;
    }

    public Stamp requested() {
        return requested;
    }

    public List<Suggestion> suggestions() {
        return List.copyOf(suggestions);
    }

    public List<RejectedProposal> rejectedProposals() {
        return rejectedProposals;
    }

    List<AuditEntry> pullAuditEntries() {
        return auditTrail.pull();
    }

    /** Los guardrails se aplican en el orden en que están declarados; el primero que rechaza es el motivo. */
    private Optional<GuardrailRejection> firstRejectionOf(ProposedFinding proposal, InspectionUnderReview inspection) {
        return Arrays.stream(GuardrailRejection.values())
                .filter(guardrail -> guardrail.rejects(proposal, inspection))
                .findFirst();
    }

    private List<Suggestion> numbered(List<ProposedFinding> admitted) {
        return new ArrayList<>(IntStream.range(0, admitted.size())
                .mapToObj(index -> new Suggestion(index + 1, admitted.get(index), new PendingDecision()))
                .toList());
    }

    private void replace(Suggestion decided) {
        suggestions.set(suggestions.indexOf(suggestion(decided.position())), decided);
    }

    private String describe(Suggestion suggestion) {
        return "Suggestion " + suggestion.position() + " on " + suggestion.proposal().criterion().value();
    }
}
