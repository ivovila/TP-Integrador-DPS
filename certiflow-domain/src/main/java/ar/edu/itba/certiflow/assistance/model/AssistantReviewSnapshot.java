package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.util.List;

public record AssistantReviewSnapshot(ReviewId id, InspectionId inspectionId, PromptVersion promptVersion, ModelId modelId,
                                      Stamp requested, List<Suggestion> suggestions,
                                      List<RejectedProposal> rejectedProposals) {

    public AssistantReviewSnapshot {
        suggestions = List.copyOf(suggestions);
        rejectedProposals = List.copyOf(rejectedProposals);
    }
}
