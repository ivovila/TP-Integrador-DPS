package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.util.List;

public record AssistantReviewSnapshot(ReviewId id, InspectionId inspectionId, PromptVersion promptVersion, ModelId modelId,
                                      Stamp requested, List<Suggestion> suggestions,
                                      List<RejectedProposal> rejectedProposals, ReviewInput reviewedInput) {

    public AssistantReviewSnapshot(ReviewId id, InspectionId inspectionId, PromptVersion promptVersion, ModelId modelId,
            Stamp requested, List<Suggestion> suggestions, List<RejectedProposal> rejectedProposals) {
        this(id, inspectionId, promptVersion, modelId, requested, suggestions, rejectedProposals, new UnavailableReviewInput());
    }

    public AssistantReviewSnapshot {
        suggestions = List.copyOf(suggestions);
        rejectedProposals = List.copyOf(rejectedProposals);
    }
}
