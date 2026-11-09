package ar.edu.itba.certiflow.assistance.usecase;

import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.util.List;

public interface AssistantReviewHistoryUseCase {

    List<AuditEntry> execute(ReviewId reviewId);
}