package ar.edu.itba.certiflow.application.assistance;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.assistance.model.exceptions.AssistantReviewNotFoundException;
import ar.edu.itba.certiflow.assistance.repository.AssistantReviewRepository;
import ar.edu.itba.certiflow.assistance.usecase.FindAssistantReviewUseCase;

public final class FindAssistantReviewUseCaseImpl implements FindAssistantReviewUseCase {

    private final AssistantReviewRepository reviewRepository;

    public FindAssistantReviewUseCaseImpl(AssistantReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public AssistantReview execute(ReviewId id) {
        return reviewRepository.findById(id).orElseThrow(() -> new AssistantReviewNotFoundException(id));
    }
}
