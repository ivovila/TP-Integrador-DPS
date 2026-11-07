package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.shared.model.Stamp;

public final class AssistantReviewFactory {

    public AssistantReview create(ReviewId id, InspectionUnderReview inspection, AssistantReply reply, Stamp requested) {
        return new AssistantReview(id, inspection, reply, requested);
    }

    public AssistantReview restore(AssistantReviewSnapshot snapshot) {
        return new AssistantReview(snapshot);
    }
}
