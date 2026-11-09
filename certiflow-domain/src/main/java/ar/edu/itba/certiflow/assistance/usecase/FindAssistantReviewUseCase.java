package ar.edu.itba.certiflow.assistance.usecase;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.ReviewId;

public interface FindAssistantReviewUseCase {

    AssistantReview execute(ReviewId id);
}