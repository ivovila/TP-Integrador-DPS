package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.application.exceptions.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.application.exceptions.BlockingFindingsPreventCertificationException;
import ar.edu.itba.certiflow.application.report.ActLine;
import ar.edu.itba.certiflow.application.report.CertificateDocument;
import ar.edu.itba.certiflow.application.report.FindingsSummary;
import ar.edu.itba.certiflow.application.report.InspectionAct;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateStatus;
import ar.edu.itba.certiflow.models.evaluation.OptionAnswer;
import ar.edu.itba.certiflow.models.evaluation.Outcome;
import ar.edu.itba.certiflow.models.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.models.finding.CorrectiveAction;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.VerificationResult;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionAudit;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndToEndFlowTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void assetGoesFromInspectionToCertificateAndEveryReportTellsTheSameStory() {
        Inspection inspection = fixture.assignedInspection();
        fixture.recordAnswer.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("5.20"), fixture.inspector);
        fixture.attachEvidence.execute(inspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector);
        fixture.addObservation.execute(inspection, fixture.pressureCriterion, "Needle sits below the green band", fixture.inspector);
        fixture.recordAnswer.execute(inspection, fixture.sealIntactCriterion, YesNoAnswer.YES, fixture.inspector);
        fixture.recordAnswer.execute(inspection, fixture.signageCriterion, new OptionAnswer("FADED"), fixture.inspector);

        List<Finding> findings = fixture.closeInspection.execute(inspection, fixture.inspector);

        InspectionAct inspectionAct = fixture.generateInspectionAct.execute(inspection);
        assertEquals("EXT-0042", inspectionAct.assetCode().value());
        assertEquals(fixture.inspector, inspectionAct.assignment().inspector());
        assertEquals(1, inspectionAct.schemaVersion());
        assertEquals(List.of(Outcome.REJECTED, Outcome.APPROVED, Outcome.OBSERVED),
                inspectionAct.lines().stream().map(ActLine::outcome).toList());
        ActLine pressureLine = inspectionAct.lines().getFirst();
        assertEquals(fixture.numericAnswer("5.20"), pressureLine.answer());
        assertEquals(fixture.inspector, pressureLine.answeredBy());
        assertEquals(1, pressureLine.evidenceCount());
        assertEquals(List.of("Needle sits below the green band"), pressureLine.observations());
        assertEquals(List.of(InspectionAudit.ASSIGNED, InspectionAudit.ANSWER_RECORDED,
                        InspectionAudit.EVIDENCE_ATTACHED, InspectionAudit.OBSERVATION_ADDED,
                        InspectionAudit.ANSWER_RECORDED, InspectionAudit.ANSWER_RECORDED, InspectionAudit.CLOSED),
                inspectionAct.history().stream().map(AuditEntry::action).toList());

        FindingsSummary summaryAfterClosing = fixture.generateFindingsSummary.execute(fixture.extinguisher);
        assertEquals(2, summaryAfterClosing.openFindings());
        assertEquals(1, summaryAfterClosing.blockingFindings());
        assertThrows(BlockingFindingsPreventCertificationException.class, () -> fixture.issueCertificate.execute(
                inspection, CertiflowFixture.TODAY.plusYears(1), fixture.certifier));

        Finding lowPressureFinding = findings.getFirst();
        CorrectiveAction rechargeAction = fixture.planCorrectiveAction.execute(lowPressureFinding, "Recharge the extinguisher",
                fixture.maintenanceTechnician, CertiflowFixture.TODAY.plusDays(10), fixture.assetResponsible);
        fixture.clock.advanceDays(7);
        fixture.verifyCorrectiveAction.execute(lowPressureFinding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED,
                "Gauge reads 7 bar after recharge");

        FindingsSummary summaryAfterVerification = fixture.generateFindingsSummary.execute(fixture.extinguisher);
        assertEquals(1, summaryAfterVerification.openFindings());
        assertEquals(0, summaryAfterVerification.blockingFindings());

        LocalDate validUntil = LocalDate.now(fixture.clock).plusYears(1);
        Certificate certificate = fixture.issueCertificate.execute(inspection, validUntil, fixture.certifier);

        CertificateDocument certificateDocument = fixture.generateCertificateDocument.execute(certificate);
        assertEquals(certificate.number(), certificateDocument.number());
        assertEquals("EXT-0042", certificateDocument.assetCode().value());
        assertEquals(CertificateStatus.ACTIVE, certificateDocument.status());
        assertEquals(validUntil, certificateDocument.validity().validUntil());
        assertEquals(fixture.certifier, certificateDocument.issuedBy());
        assertEquals(List.of(inspection), fixture.inspectionRepository.findByAsset(fixture.extinguisher));
        assertTrue(fixture.certificateRepository.findCurrentFor(fixture.extinguisher, LocalDate.now(fixture.clock)).isPresent());
    }

    @Test
    void actIsOnlyIssuedForClosedInspections() {
        Inspection inspectionInProgress = fixture.assignedInspection();

        assertThrows(InspectionNotClosedException.class, () -> fixture.generateInspectionAct.execute(inspectionInProgress));
    }

    @Test
    void catalogRejectsASecondAssetWithTheSameCode() {
        assertThrows(AssetAlreadyRegisteredException.class, () -> fixture.registerAsset.execute(fixture.extinguisher));
        assertEquals(fixture.extinguisher, fixture.assetRepository.findByCode(fixture.extinguisher.code()).orElseThrow());
    }
}
