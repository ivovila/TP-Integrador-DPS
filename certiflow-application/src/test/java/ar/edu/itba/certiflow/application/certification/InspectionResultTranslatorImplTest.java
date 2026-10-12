package ar.edu.itba.certiflow.application.certification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.OpenNonConformity;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InspectionResultTranslatorImplTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Mock
    private InspectionRepository inspectionRepository;
    @Mock
    private FindingRepository findingRepository;

    @Test
    void onlyOpenFindingsReachCertificationWithTheirSeverity() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "5.00", YesNoAnswer.YES, "VISIBLE");
        fixture.close(inspection, fixture.inspector);
        when(inspectionRepository.findById(inspection)).thenReturn(Optional.of(fixture.inspection(inspection)));
        when(findingRepository.findByAsset(fixture.extinguisher.code()))
                .thenReturn(fixture.findingRepository.findByAsset(fixture.extinguisher.code()));

        InspectionResult result = new InspectionResultTranslatorImpl(inspectionRepository, findingRepository).translate(inspection);

        assertEquals(List.of(new OpenNonConformity("EXT-01", StandardSeverity.MAJOR)), result.openNonConformities());
        assertEquals(fixture.inspection(inspection).originalRevision().sealedAt(), result.closedAt().orElseThrow());
    }

    @Test
    void anOpenInspectionTranslatesToAResultWithoutClosingInstant() {
        InspectionId inspection = fixture.assignedInspection();
        when(inspectionRepository.findById(inspection)).thenReturn(Optional.of(fixture.inspection(inspection)));
        when(findingRepository.findByAsset(fixture.extinguisher.code())).thenReturn(List.of());

        assertFalse(new InspectionResultTranslatorImpl(inspectionRepository, findingRepository).translate(inspection).isClosed());
    }

    @Test
    void anUnknownInspectionCannotBeTranslated() {
        InspectionId unknown = new InspectionId("missing");
        when(inspectionRepository.findById(unknown)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(InspectionNotFoundException.class,
                () -> new InspectionResultTranslatorImpl(inspectionRepository, findingRepository).translate(unknown));
    }
}
