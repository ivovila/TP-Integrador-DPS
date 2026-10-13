package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateFactory;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingFactory;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionFactory;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import org.junit.jupiter.api.Test;
import java.util.List;

class SnapshotTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void aRestoredInspectionHasTheSameStateAndNoPendingAuditEntries() {
        InspectionId id = fixture.closedCompliantInspection();
        Inspection original = fixture.inspection(id);

        Inspection restored = new InspectionFactory().restore(original.snapshot());

        assertEquals(original.snapshot(), restored.snapshot());
        assertTrue(restored.isClosed());
        int entriesBefore = fixture.inspectionLog.historyOf(id).size();
        fixture.inspectionRepository.save(restored);
        assertThrows(InspectionAlreadyClosedException.class,
                () -> fixture.answer(id, fixture.sealIntactCriterion, YesNoAnswer.NO, fixture.inspector));
        assertEquals(entriesBefore, fixture.inspectionLog.historyOf(id).size());
    }

    @Test
    void aRestoredFindingKeepsNumberingItsActions() {
        FindingId id = fixture.majorFindingOf(fixture.assignedInspection());
        fixture.plan(id, "Recharge", fixture.maintenanceTechnician, CertiflowFixture.TODAY.plusDays(5), fixture.planner);
        Finding original = fixture.finding(id);

        Finding restored = new FindingFactory().restore(original.snapshot());
        fixture.findingRepository.save(restored);
        CorrectiveActionId next = fixture.plan(id, "Replace", fixture.maintenanceTechnician, CertiflowFixture.TODAY.plusDays(6),
                fixture.planner);

        assertEquals(new CorrectiveActionId(2), next);
        assertEquals(original.snapshot().actions().getFirst(), fixture.finding(id).snapshot().actions().getFirst());
    }

    @Test
    void aRestoredCertificateKeepsItsLifecycle() {
        CertificateNumber number = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);
        fixture.suspend(number, "Seal tampered", fixture.planner);
        Certificate original = fixture.certificate(number);

        Certificate restored = new CertificateFactory().restore(original.snapshot());

        assertEquals(original.snapshot(), restored.snapshot());
        assertEquals(original.statusOn(CertiflowFixture.TODAY, List.of()), restored.statusOn(CertiflowFixture.TODAY, List.of()));
    }
}
