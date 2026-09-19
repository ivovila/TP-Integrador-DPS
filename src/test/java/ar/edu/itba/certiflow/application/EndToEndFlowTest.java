package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.application.asset.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.application.certificate.BlockingFindingsPreventCertificationException;
import ar.edu.itba.certiflow.application.report.ActLine;
import ar.edu.itba.certiflow.application.report.CertificateDocument;
import ar.edu.itba.certiflow.application.report.FindingsSummary;
import ar.edu.itba.certiflow.application.report.InspectionAct;
import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.domain.certificate.Certificate;
import ar.edu.itba.certiflow.domain.certificate.CertificateStatus;
import ar.edu.itba.certiflow.domain.evaluation.OptionAnswer;
import ar.edu.itba.certiflow.domain.evaluation.Outcome;
import ar.edu.itba.certiflow.domain.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.domain.finding.CorrectiveAction;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.finding.VerificationResult;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.InspectionAudit;
import ar.edu.itba.certiflow.domain.inspection.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndToEndFlowTest {

    private final CertiflowFixture app = new CertiflowFixture();

    @Test
    void assetGoesFromInspectionToCertificateAndEveryReportTellsTheSameStory() {
        Inspection inspection = app.assignedInspection();
        app.recordAnswer.execute(inspection, app.pressure, app.numericAnswer("5.20"), app.inspector);
        app.attachEvidence.execute(inspection, app.pressure, app.gaugePhoto(), app.inspector);
        app.addObservation.execute(inspection, app.pressure, "Needle sits below the green band", app.inspector);
        app.recordAnswer.execute(inspection, app.sealIntact, YesNoAnswer.YES, app.inspector);
        app.recordAnswer.execute(inspection, app.signage, new OptionAnswer("FADED"), app.inspector);

        List<Finding> findings = app.closeInspection.execute(inspection, app.inspector);

        InspectionAct act = app.generateInspectionAct.execute(inspection);
        assertEquals("EXT-0042", act.assetCode().value());
        assertEquals(app.inspector, act.assignment().inspector());
        assertEquals(1, act.schemaVersion());
        assertEquals(List.of(Outcome.REJECTED, Outcome.APPROVED, Outcome.OBSERVED),
                act.lines().stream().map(ActLine::outcome).toList());
        ActLine pressureLine = act.lines().getFirst();
        assertEquals(app.numericAnswer("5.20"), pressureLine.answer());
        assertEquals(app.inspector, pressureLine.answeredBy());
        assertEquals(1, pressureLine.evidenceCount());
        assertEquals(List.of("Needle sits below the green band"), pressureLine.observations());
        assertEquals(List.of(InspectionAudit.ASSIGNED, InspectionAudit.ANSWER_RECORDED,
                        InspectionAudit.EVIDENCE_ATTACHED, InspectionAudit.OBSERVATION_ADDED,
                        InspectionAudit.ANSWER_RECORDED, InspectionAudit.ANSWER_RECORDED, InspectionAudit.CLOSED),
                act.history().stream().map(AuditEntry::action).toList());

        FindingsSummary afterClosing = app.generateFindingsSummary.execute(app.extinguisher);
        assertEquals(2, afterClosing.openFindings());
        assertEquals(1, afterClosing.blockingFindings());
        assertThrows(BlockingFindingsPreventCertificationException.class, () -> app.issueCertificate.execute(
                inspection, CertiflowFixture.TODAY.plusYears(1), app.certifier));

        Finding lowPressure = findings.getFirst();
        CorrectiveAction recharge = app.planCorrectiveAction.execute(lowPressure, "Recharge the extinguisher",
                app.maintenance, CertiflowFixture.TODAY.plusDays(10), app.assetResponsible);
        app.clock.advanceDays(7);
        app.verifyCorrectiveAction.execute(lowPressure, recharge, app.verifier, VerificationResult.ACCEPTED,
                "Gauge reads 7 bar after recharge");

        FindingsSummary afterVerification = app.generateFindingsSummary.execute(app.extinguisher);
        assertEquals(1, afterVerification.openFindings());
        assertEquals(0, afterVerification.blockingFindings());

        LocalDate validUntil = LocalDate.now(app.clock).plusYears(1);
        Certificate certificate = app.issueCertificate.execute(inspection, validUntil, app.certifier);

        CertificateDocument document = app.generateCertificateDocument.execute(certificate);
        assertEquals(certificate.number(), document.number());
        assertEquals("EXT-0042", document.assetCode().value());
        assertEquals(CertificateStatus.ACTIVE, document.status());
        assertEquals(validUntil, document.validity().to());
        assertEquals(app.certifier, document.issuedBy());
        assertEquals(List.of(inspection), app.inspections.findByAsset(app.extinguisher));
        assertTrue(app.certificates.findCurrentFor(app.extinguisher, LocalDate.now(app.clock)).isPresent());
    }

    @Test
    void actIsOnlyIssuedForClosedInspections() {
        Inspection inProgress = app.assignedInspection();

        assertThrows(InspectionNotClosedException.class, () -> app.generateInspectionAct.execute(inProgress));
    }

    @Test
    void catalogRejectsASecondAssetWithTheSameCode() {
        assertThrows(AssetAlreadyRegisteredException.class, () -> app.registerAsset.execute(app.extinguisher));
        assertEquals(app.extinguisher, app.assets.findByCode(app.extinguisher.code()).orElseThrow());
    }
}
