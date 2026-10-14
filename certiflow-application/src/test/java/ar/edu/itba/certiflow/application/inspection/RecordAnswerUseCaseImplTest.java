package ar.edu.itba.certiflow.application.inspection;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerCommand;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordAnswerUseCaseImplTest {

    @Mock
    private InspectionRepository inspectionRepository;
    @Mock
    private AssetLock assetLock;
    private RecordAnswerUseCaseImpl recordAnswer;

    @BeforeEach
    void setUp() {
        recordAnswer = new RecordAnswerUseCaseImpl(inspectionRepository, Samples.inspectionService(), assetLock, Samples.CLOCK);
    }

    /** Quien cierra la inspección bloquea el mismo activo: leerla antes de bloquear dejaría responder una ya cerrada. */
    @Test
    void theAssetIsLockedBeforeTheInspectionIsLoadedAndTheAnswerIsSaved() {
        Inspection inspection = Samples.answeredInspection("7.00");
        when(inspectionRepository.assetCodeOf(inspection.id())).thenReturn(Optional.of(inspection.assetCode()));
        when(inspectionRepository.findById(inspection.id())).thenReturn(Optional.of(inspection));

        recordAnswer.execute(new RecordAnswerCommand(inspection.id(), new CriterionCode("EXT-02"), new RawAnswer("false"),
                Samples.INSPECTOR));

        InOrder order = inOrder(inspectionRepository, assetLock);
        order.verify(inspectionRepository).assetCodeOf(inspection.id());
        order.verify(assetLock).lock(inspection.assetCode());
        order.verify(inspectionRepository).findById(inspection.id());
        order.verify(inspectionRepository).save(inspection);
    }

    @Test
    void anUnknownInspectionIsRejectedWithoutLockingAnything() {
        InspectionId unknown = new InspectionId("missing");
        when(inspectionRepository.assetCodeOf(unknown)).thenReturn(Optional.empty());

        assertThrows(InspectionNotFoundException.class, () -> recordAnswer.execute(
                new RecordAnswerCommand(unknown, new CriterionCode("EXT-02"), new RawAnswer("true"), Samples.INSPECTOR)));
        verifyNoInteractions(assetLock);
        verify(inspectionRepository, never()).save(any());
    }
}
