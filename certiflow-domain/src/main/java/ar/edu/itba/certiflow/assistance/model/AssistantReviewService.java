package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Stamp;

/** Único punto de entrada a los comandos de una revisión asistida, y quien deja su bitácora. */
public final class AssistantReviewService {

    private final AssistantReviewFactory reviewFactory;
    private final AuditLog<ReviewId> reviewLog;

    public AssistantReviewService(AssistantReviewFactory reviewFactory, AuditLog<ReviewId> reviewLog) {
        this.reviewFactory = reviewFactory;
        this.reviewLog = reviewLog;
    }

    public AssistantReview record(ReviewId id, InspectionUnderReview inspection, AssistantReply reply, Stamp requested) {
        return record(id, inspection, inspection, reply, requested);
    }

    public AssistantReview record(ReviewId id, InspectionUnderReview reviewed, InspectionUnderReview current,
                                  AssistantReply reply, Stamp requested) {
        AssistantReview review = reviewFactory.create(id, reviewed, current, reply, requested);
        recordAuditEntriesOf(review);
        return review;
    }

    public void accept(AssistantReview review, int position, Stamp accepted) {
        review.accept(position, accepted);
        recordAuditEntriesOf(review);
    }

    public void dismiss(AssistantReview review, int position, String reason, Stamp dismissed) {
        review.dismiss(position, reason, dismissed);
        recordAuditEntriesOf(review);
    }

    private void recordAuditEntriesOf(AssistantReview review) {
        reviewLog.recordAll(review.id(), review.pullAuditEntries());
    }
}
