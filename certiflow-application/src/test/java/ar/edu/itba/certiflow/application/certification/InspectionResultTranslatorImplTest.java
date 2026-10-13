package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.shared.model.Stamp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.OpenNonConformity;
import ar.edu.itba.certiflow.inspection.model.ActionPlan;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionClosed;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InspectionResultTranslatorImplTest {

    @Mock
    private InspectionRepository inspectionRepository;
    @Mock
    private FindingRepository findingRepository;
    @Mock
    private AssetRepository assetRepository;
    @InjectMocks
    private InspectionResultTranslatorImpl translator;

    @Test
    void onlyOpenFindingsReachCertificationWithTheirSeverity() {
        Inspection inspection = Samples.answeredInspection("5.00");
        InspectionClosed closure = Samples.inspectionService().close(inspection, new Stamp(Samples.INSPECTOR, Samples.NOW));
        FindingService findings = Samples.findingService();
        Finding open = findings.raiseFrom(closure, () -> new FindingId("F-open")).getFirst();
        Finding closed = findings.raiseFrom(closure, () -> new FindingId("F-closed")).getFirst();
        findings.planAction(closed, new ActionPlan("Recharge", Samples.PLANNER, LocalDate.of(2026, 3, 20)), new Stamp(Samples.RESPONSIBLE, Samples.NOW),
                LocalDate.of(2026, 3, 10));
        findings.verifyAction(closed, closed.actions().getFirst().id(), VerificationResult.ACCEPTED, "Done",
                new Stamp(Samples.INSPECTOR, Samples.NOW));
        when(inspectionRepository.findById(inspection.id())).thenReturn(Optional.of(inspection));
        when(assetRepository.findByCode(Samples.ASSET.code())).thenReturn(Optional.of(Samples.ASSET));
        when(findingRepository.findByAsset(Samples.ASSET.code())).thenReturn(List.of(open, closed));

        InspectionResult result = translator.translate(inspection.id());

        assertEquals(List.of(new OpenNonConformity("EXT-01", StandardSeverity.MAJOR, "F-open", Optional.empty())),
                result.openNonConformities());
        assertEquals(Samples.NOW, result.closedAt().orElseThrow());
    }

    @Test
    void anOpenInspectionTranslatesToAResultWithoutClosingInstant() {
        Inspection inspection = Samples.answeredInspection("7.00");
        when(inspectionRepository.findById(inspection.id())).thenReturn(Optional.of(inspection));
        when(assetRepository.findByCode(Samples.ASSET.code())).thenReturn(Optional.of(Samples.ASSET));
        when(findingRepository.findByAsset(Samples.ASSET.code())).thenReturn(List.of());

        assertFalse(translator.translate(inspection.id()).isClosed());
    }

    @Test
    void anUnknownInspectionCannotBeTranslated() {
        InspectionId unknown = new InspectionId("missing");
        when(inspectionRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThrows(InspectionNotFoundException.class, () -> translator.translate(unknown));
    }
}
