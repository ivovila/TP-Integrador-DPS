package ar.edu.itba.certiflow.application.assistance;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.InspectionReviewTranslator;
import ar.edu.itba.certiflow.assistance.repository.AssistantReviewRepository;
import ar.edu.itba.certiflow.assistance.usecase.ListAssistantReviewsUseCase;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.util.List;

public final class ListAssistantReviewsUseCaseImpl implements ListAssistantReviewsUseCase {

    private final AssistantReviewRepository reviewRepository;
    private final InspectionReviewTranslator translator;

    public ListAssistantReviewsUseCaseImpl(AssistantReviewRepository reviewRepository, InspectionReviewTranslator translator) {
        this.reviewRepository = reviewRepository;
        this.translator = translator;
    }

    /** Una inspección que no existe es un 404, no una lista vacía. */
    @Override
    public List<AssistantReview> execute(InspectionId inspectionId) {
        translator.assetCodeOf(inspectionId);
        return reviewRepository.findByInspection(inspectionId);
    }
}
