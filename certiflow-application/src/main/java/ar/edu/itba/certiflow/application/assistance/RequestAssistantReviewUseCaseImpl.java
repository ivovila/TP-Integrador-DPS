package ar.edu.itba.certiflow.application.assistance;

import ar.edu.itba.certiflow.assistance.model.AssistantReply;
import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewService;
import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.assistance.model.InspectionReviewTranslator;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.assistance.repository.AssistantReviewRepository;
import ar.edu.itba.certiflow.assistance.usecase.RequestAssistantReviewCommand;
import ar.edu.itba.certiflow.assistance.usecase.RequestAssistantReviewUseCase;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.shared.model.Transactions;
import java.time.Clock;

/**
 * Pedir una revisión son tres pasos, y la llamada al modelo queda afuera de toda transacción: tarda segundos y no
 * puede tener una conexión de la base ocupada mientras tanto. El tercer paso vuelve a leer la inspección, así los
 * guardrails y el control de que siga abierta se hacen sobre lo que hay al guardar.
 */
public final class RequestAssistantReviewUseCaseImpl implements RequestAssistantReviewUseCase {

    private final InspectionReviewTranslator translator;
    private final InspectionAssistant assistant;
    private final AssistantReviewRepository reviewRepository;
    private final AssistantReviewService reviewService;
    private final AssetLock assetLock;
    private final Transactions transactions;
    private final Clock clock;

    public RequestAssistantReviewUseCaseImpl(InspectionReviewTranslator translator, InspectionAssistant assistant,
                                             AssistantReviewRepository reviewRepository,
                                             AssistantReviewService reviewService, AssetLock assetLock,
                                             Transactions transactions, Clock clock) {
        this.translator = translator;
        this.assistant = assistant;
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
        this.assetLock = assetLock;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public ReviewId execute(RequestAssistantReviewCommand command) {
        InspectionUnderReview inspection = transactions.within(() -> openInspection(command.inspectionId()));
        AssistantReply reply = assistant.review(inspection);
        return transactions.within(() -> record(command, inspection, reply));
    }

    private InspectionUnderReview openInspection(InspectionId inspectionId) {
        InspectionUnderReview inspection = translator.translate(inspectionId);
        inspection.ensureOpen();
        return inspection;
    }

    private ReviewId record(RequestAssistantReviewCommand command, InspectionUnderReview reviewed, AssistantReply reply) {
        assetLock.lock(translator.assetCodeOf(command.inspectionId()));
        InspectionUnderReview current = translator.translate(command.inspectionId());
        AssistantReview review = reviewService.record(reviewRepository.nextId(), reviewed, current, reply,
                new Stamp(command.requestedBy(), clock.instant()));
        reviewRepository.save(review);
        return review.id();
    }
}
