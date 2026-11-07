package ar.edu.itba.certiflow.application.assistance;

import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.assistance.usecase.AssistantReviewHistoryUseCase;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.List;

public final class AssistantReviewHistoryUseCaseImpl implements AssistantReviewHistoryUseCase {

    private final AuditLog<ReviewId> reviewLog;

    public AssistantReviewHistoryUseCaseImpl(AuditLog<ReviewId> reviewLog) {
        this.reviewLog = reviewLog;
    }

    @Override
    public List<AuditEntry> execute(ReviewId reviewId) {
        return reviewLog.historyOf(reviewId);
    }
}
