package ar.edu.itba.certiflow.application;

import java.util.List;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.inspection.model.FullScope;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import org.junit.jupiter.api.Test;

class AggregateIdentityTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void everyInspectionGetsItsOwnIdAndIsFoundByIt() {
        InspectionId first = fixture.assignedInspection();
        InspectionId second = fixture.assignedInspection();

        assertNotEquals(first, second);
        assertEquals(first, fixture.inspection(first).id());
    }

    @Test
    void findingReferencesItsInspectionByIdAndActionsAreNumberedWithinTheFinding() {
        InspectionId inspection = fixture.assignedInspection();
        FindingId finding = fixture.majorFindingOf(inspection);

        CorrectiveActionId firstAction = fixture.plan(finding, "Recharge", fixture.maintenanceTechnician, CertiflowFixture.TODAY, fixture.assetResponsible);
        CorrectiveActionId secondAction = fixture.plan(finding, "Replace", fixture.maintenanceTechnician, CertiflowFixture.TODAY, fixture.assetResponsible);

        assertEquals(inspection, fixture.finding(finding).inspectionId());
        assertEquals(new CorrectiveActionId(1), firstAction);
        assertEquals(new CorrectiveActionId(2), secondAction);
    }

    @Test
    void useCasesRejectIdsThatDoNotExist() {
        InspectionId unknownInspection = new InspectionId("missing");
        FindingId unknownFinding = new FindingId("missing");
        CertificateNumber unknownCertificate = new CertificateNumber("CERT-99999");

        assertThrows(InspectionNotFoundException.class, () -> fixture.answer(unknownInspection, fixture.sealIntactCriterion, YesNoAnswer.YES, fixture.inspector));
        assertThrows(FindingNotFoundException.class, () -> fixture.plan(unknownFinding, "Recharge", fixture.maintenanceTechnician, CertiflowFixture.TODAY, fixture.assetResponsible));
        assertThrows(CertificateNotFoundException.class,
                () -> fixture.suspend(unknownCertificate, "Discharged", fixture.certifier));
        assertThrows(AssetNotFoundException.class, () -> fixture.assignInspection.execute(new AssignInspectionCommand(new AssetCode("EXT-9999"), (new InspectionAssignment(fixture.inspector, CertiflowFixture.TODAY, new FullScope())).inspector(), (new InspectionAssignment(fixture.inspector, CertiflowFixture.TODAY, new FullScope())).plannedDate(), (new InspectionAssignment(fixture.inspector, CertiflowFixture.TODAY, new FullScope())).scope(), fixture.planner)));
    }

    @Test
    void historyIsFoundByIdentityEvenFromAnotherInstanceOfTheSameAggregate() {
        InspectionId inspection = fixture.assignedInspection();

        assertEquals(List.of(InspectionAudit.ASSIGNED),
                fixture.inspectionLog.historyOf(inspection).stream().map(AuditEntry::action).toList());
    }
}
