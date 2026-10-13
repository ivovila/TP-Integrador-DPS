package ar.edu.itba.certiflow.application.inspection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionCommand;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloseInspectionUseCaseImplTest {

    @Mock
    private InspectionRepository inspectionRepository;
    @Mock
    private FindingRepository findingRepository;
    private CloseInspectionUseCaseImpl closeInspection;

    @BeforeEach
    void setUp() {
        closeInspection = new CloseInspectionUseCaseImpl(inspectionRepository, findingRepository, Samples.inspectionService(),
                Samples.findingService(), Samples.CLOCK);
    }

    @Test
    void closingSavesTheInspectionAndEveryFindingItRaises() {
        Inspection inspection = Samples.answeredInspection("5.00");
        when(inspectionRepository.findById(inspection.id())).thenReturn(Optional.of(inspection));
        when(findingRepository.nextId()).thenReturn(new FindingId("F-1"));

        List<FindingId> raised = closeInspection.execute(new CloseInspectionCommand(inspection.id(), Samples.INSPECTOR));

        assertEquals(List.of(new FindingId("F-1")), raised);
        assertTrue(inspection.isClosed());
        verify(inspectionRepository).save(inspection);
        verify(findingRepository).saveAll(any());
    }

    @Test
    void anUnknownInspectionIsRejectedAndNothingIsSaved() {
        InspectionId unknown = new InspectionId("missing");
        when(inspectionRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(InspectionNotFoundException.class,
                () -> closeInspection.execute(new CloseInspectionCommand(unknown, Samples.INSPECTOR)));
        verify(inspectionRepository, never()).save(any());
        verify(findingRepository, never()).saveAll(any());
    }
}
