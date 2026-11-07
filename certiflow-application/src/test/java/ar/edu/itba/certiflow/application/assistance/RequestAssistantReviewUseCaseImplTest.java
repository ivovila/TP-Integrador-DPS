package ar.edu.itba.certiflow.application.assistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.assistance.model.AssistantReply;
import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewFactory;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewService;
import ar.edu.itba.certiflow.assistance.model.CriterionUnderReview;
import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.assistance.model.InspectionReviewTranslator;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ModelId;
import ar.edu.itba.certiflow.assistance.model.PromptVersion;
import ar.edu.itba.certiflow.assistance.model.ProposedFinding;
import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.assistance.model.exceptions.AssistantUnavailableException;
import ar.edu.itba.certiflow.assistance.model.exceptions.ReviewOfClosedInspectionException;
import ar.edu.itba.certiflow.assistance.repository.AssistantReviewRepository;
import ar.edu.itba.certiflow.assistance.usecase.RequestAssistantReviewCommand;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.shared.model.Transactions;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class RequestAssistantReviewUseCaseImplTest {

    private final InspectionId inspectionId = new InspectionId("I-1");
    private final InspectionReviewTranslator translator = mock(InspectionReviewTranslator.class);
    private final InspectionAssistant assistant = mock(InspectionAssistant.class);
    private final AssistantReviewRepository reviewRepository = mock(AssistantReviewRepository.class);
    private final AssetLock assetLock = mock(AssetLock.class);
    private final CountingTransactions transactions = new CountingTransactions();
    private final RequestAssistantReviewUseCaseImpl useCase = new RequestAssistantReviewUseCaseImpl(translator, assistant,
            reviewRepository, new AssistantReviewService(new AssistantReviewFactory(), Samples.auditLog()), assetLock,
            transactions, Samples.CLOCK);
    private final RequestAssistantReviewCommand command = new RequestAssistantReviewCommand(inspectionId, Samples.INSPECTOR);

    private final AssistantReply reply = new AssistantReply(new PromptVersion("v1"), new ModelId("stub"), List.of(
            new ProposedFinding(new CriterionCode("EXT-02"), StandardSeverity.MAJOR, "The pin is loose",
                    List.of(new Quote("pin is loose")))));

    private InspectionUnderReview inspection(boolean closed, Outcome sealOutcome) {
        return new InspectionUnderReview(inspectionId, Samples.EXTINGUISHER, 1, closed, List.of(new CriterionUnderReview(
                new CriterionCode("EXT-02"), "Seal intact", "yes-no (expected: YES)", "true", sealOutcome,
                List.of("The pin is loose"), List.of())));
    }

    @BeforeEach
    void anInspectionOfTheSampleAsset() {
        when(translator.assetCodeOf(inspectionId)).thenReturn(Samples.ASSET.code());
        when(reviewRepository.nextId()).thenReturn(new ReviewId("R-1"));
    }

    @Test
    void theModelIsCalledBetweenTwoTransactionsAndTheReviewIsSavedInTheSecond() {
        when(translator.translate(inspectionId)).thenReturn(inspection(false, Outcome.APPROVED));
        when(assistant.review(any())).then(invocation -> {
            assertEquals(1, transactions.finished);
            assertEquals(0, transactions.open);
            return reply;
        });

        ReviewId reviewId = useCase.execute(command);

        assertEquals(new ReviewId("R-1"), reviewId);
        assertEquals(2, transactions.finished);
        InOrder order = inOrder(translator, assistant, assetLock, reviewRepository);
        order.verify(translator).translate(inspectionId);
        order.verify(assistant).review(any());
        order.verify(assetLock).lock(Samples.ASSET.code());
        order.verify(translator).translate(inspectionId);
        order.verify(reviewRepository).save(any());
    }

    @Test
    void aClosedInspectionIsRejectedBeforeCallingTheModel() {
        when(translator.translate(inspectionId)).thenReturn(inspection(true, Outcome.APPROVED));

        assertThrows(ReviewOfClosedInspectionException.class, () -> useCase.execute(command));

        verifyNoInteractions(assistant);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void anInspectionClosedWhileTheModelAnsweredIsRejectedAndNothingIsSaved() {
        when(translator.translate(inspectionId)).thenReturn(inspection(false, Outcome.APPROVED), inspection(true, Outcome.APPROVED));
        when(assistant.review(any())).thenReturn(reply);

        assertThrows(ReviewOfClosedInspectionException.class, () -> useCase.execute(command));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void anUnavailableModelSavesNothing() {
        when(translator.translate(inspectionId)).thenReturn(inspection(false, Outcome.APPROVED));
        when(assistant.review(any())).thenThrow(new AssistantUnavailableException("The model did not answer"));

        assertThrows(AssistantUnavailableException.class, () -> useCase.execute(command));

        assertEquals(1, transactions.finished);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void theGuardrailsJudgeTheInspectionAsItIsWhenSaving() {
        when(translator.translate(inspectionId)).thenReturn(inspection(false, Outcome.APPROVED), inspection(false, Outcome.REJECTED));
        when(assistant.review(any())).thenReturn(reply);

        useCase.execute(command);

        ArgumentCaptor<AssistantReview> saved = ArgumentCaptor.forClass(AssistantReview.class);
        verify(reviewRepository).save(saved.capture());
        assertEquals(List.of(), saved.getValue().suggestions());
        assertEquals(1, saved.getValue().rejectedProposals().size());
    }

    /** Corre el trabajo en el acto y cuenta cuántas transacciones hay abiertas y cuántas terminaron. */
    private static final class CountingTransactions implements Transactions {

        private int open;
        private int finished;

        @Override
        public <T> T within(Supplier<T> work) {
            open++;
            T result = work.get();
            open--;
            finished++;
            return result;
        }
    }
}
