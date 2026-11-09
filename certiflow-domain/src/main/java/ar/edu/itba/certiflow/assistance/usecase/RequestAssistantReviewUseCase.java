package ar.edu.itba.certiflow.assistance.usecase;

import ar.edu.itba.certiflow.assistance.model.ReviewId;

public interface RequestAssistantReviewUseCase {

    ReviewId execute(RequestAssistantReviewCommand command);
}