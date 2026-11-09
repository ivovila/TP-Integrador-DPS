package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.shared.model.Stamp;

public final class AssistantReviewFactory {

    public AssistantReview create(ReviewId id, InspectionUnderReview inspection, AssistantReply reply, Stamp requested) {
        return new AssistantReview(id, inspection, reply, requested);
    }

    public AssistantReview create(ReviewId id, InspectionUnderReview reviewed, InspectionUnderReview current,
                                  AssistantReply reply, Stamp requested) {
        if (!reviewed.inspectionId().equals(current.inspectionId())) {
            throw new IllegalArgumentException("A review must keep the identity of its input");
        }
        return new AssistantReview(id, current, reply, requested, new CapturedReviewInput(reviewed));
    }

    public AssistantReview restore(AssistantReviewSnapshot snapshot) {
        return new AssistantReview(snapshot);
    }
}
