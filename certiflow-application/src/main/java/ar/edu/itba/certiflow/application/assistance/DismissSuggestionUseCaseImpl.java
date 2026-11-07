package ar.edu.itba.certiflow.application.assistance;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewService;
import ar.edu.itba.certiflow.assistance.model.InspectionReviewTranslator;
import ar.edu.itba.certiflow.assistance.model.exceptions.AssistantReviewNotFoundException;
import ar.edu.itba.certiflow.assistance.repository.AssistantReviewRepository;
import ar.edu.itba.certiflow.assistance.usecase.DismissSuggestionCommand;
import ar.edu.itba.certiflow.assistance.usecase.DismissSuggestionUseCase;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Clock;

public final class DismissSuggestionUseCaseImpl implements DismissSuggestionUseCase {

    private final AssistantReviewRepository reviewRepository;
    private final AssistantReviewService reviewService;
    private final InspectionReviewTranslator translator;
    private final AssetLock assetLock;
    private final Clock clock;

    public DismissSuggestionUseCaseImpl(AssistantReviewRepository reviewRepository, AssistantReviewService reviewService,
                                        InspectionReviewTranslator translator, AssetLock assetLock, Clock clock) {
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
        this.translator = translator;
        this.assetLock = assetLock;
        this.clock = clock;
    }

    @Override
    public void execute(DismissSuggestionCommand command) {
        InspectionId inspectionId = reviewRepository.inspectionOf(command.reviewId())
                .orElseThrow(() -> new AssistantReviewNotFoundException(command.reviewId()));
        assetLock.lock(translator.assetCodeOf(inspectionId));
        AssistantReview review = reviewRepository.findById(command.reviewId())
                .orElseThrow(() -> new AssistantReviewNotFoundException(command.reviewId()));
        reviewService.dismiss(review, command.position(), command.reason(),
                new Stamp(command.dismissedBy(), clock.instant()));
        reviewRepository.save(review);
    }
}
