package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.application.certificate.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.application.certificate.BlockingFindingsPreventCertificationException;
import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.domain.certificate.Certificate;
import ar.edu.itba.certiflow.domain.certificate.CertificateAlreadyRenewedException;
import ar.edu.itba.certiflow.domain.certificate.CertificateAudit;
import ar.edu.itba.certiflow.domain.certificate.CertificateNotActiveException;
import ar.edu.itba.certiflow.domain.certificate.CertificateStatus;
import ar.edu.itba.certiflow.domain.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.domain.finding.CorrectiveAction;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.finding.VerificationResult;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.InspectionNotClosedException;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CertificationTest {

    private static final LocalDate VALID_UNTIL = CertiflowFixture.TODAY.plusYears(1);

    private final CertiflowFixture app = new CertiflowFixture();

    @Test
    void assetWithAnOpenBlockingFindingIsNotEligible() {
        Inspection inspection = app.assignedInspection();
        app.majorFindingOf(inspection);

        assertThrows(BlockingFindingsPreventCertificationException.class,
                () -> app.issueCertificate.execute(inspection, VALID_UNTIL, app.certifier));
    }

    @Test
    void openMinorFindingsDoNotPreventCertification() {
        Inspection inspection = app.assignedInspection();
        app.answerAll(inspection, "7.00", YesNoAnswer.YES, "FADED");
        List<Finding> findings = app.closeInspection.execute(inspection, app.inspector);

        Certificate certificate = app.issueCertificate.execute(inspection, VALID_UNTIL, app.certifier);

        assertTrue(findings.getFirst().isOpen());
        assertEquals(CertificateStatus.ACTIVE, certificate.statusOn(LocalDate.now(app.clock)));
    }

    @Test
    void assetBecomesEligibleOnceItsBlockingFindingIsClosed() {
        Inspection inspection = app.assignedInspection();
        Finding finding = app.majorFindingOf(inspection);
        CorrectiveAction recharge = app.planCorrectiveAction.execute(finding, "Recharge the extinguisher",
                app.maintenance, CertiflowFixture.TODAY.plusDays(15), app.assetResponsible);
        app.verifyCorrectiveAction.execute(finding, recharge, app.verifier, VerificationResult.ACCEPTED, "Recharged");

        Certificate certificate = app.issueCertificate.execute(inspection, VALID_UNTIL, app.certifier);

        assertEquals("CERT-00001", certificate.number().value());
        assertEquals(app.extinguisher, certificate.asset());
        assertEquals(inspection, certificate.basedOn());
        AuditEntry issuance = app.auditService.entriesFor(certificate).getFirst();
        assertEquals(CertificateAudit.ISSUED, issuance.action());
        assertEquals(app.certifier, issuance.by());
    }

    @Test
    void inspectionStillInProgressCannotBackACertificate() {
        Inspection inProgress = app.assignedInspection();

        assertThrows(InspectionNotClosedException.class,
                () -> app.issueCertificate.execute(inProgress, VALID_UNTIL, app.certifier));
    }

    @Test
    void assetHoldsASingleCurrentCertificate() {
        Inspection first = app.closedCompliantInspection();
        app.issueCertificate.execute(first, VALID_UNTIL, app.certifier);
        Inspection second = app.closedCompliantInspection();

        assertThrows(AssetAlreadyCertifiedException.class,
                () -> app.issueCertificate.execute(second, VALID_UNTIL, app.certifier));
    }

    @Test
    void suspendedCertificateStillOccupiesTheAssetSoANewOneCannotBeIssuedAroundIt() {
        Certificate certificate = app.issueCertificate.execute(app.closedCompliantInspection(), VALID_UNTIL,
                app.certifier);
        app.suspendCertificate.execute(certificate, "Extinguisher discharged during a drill", app.certifier);

        assertThrows(AssetAlreadyCertifiedException.class, () -> app.issueCertificate.execute(
                app.closedCompliantInspection(), VALID_UNTIL, app.certifier));
    }

    @Test
    void certificateIsActiveThroughItsLastDayAndExpiredTheDayAfter() {
        Certificate certificate = app.issueCertificate.execute(app.closedCompliantInspection(), VALID_UNTIL,
                app.certifier);

        app.clock.moveTo(VALID_UNTIL);
        assertEquals(CertificateStatus.ACTIVE, certificate.statusOn(LocalDate.now(app.clock)));
        assertEquals(CertificateStatus.ACTIVE, app.generateCertificateDocument.execute(certificate).status());

        app.clock.advanceDays(1);
        assertEquals(CertificateStatus.EXPIRED, certificate.statusOn(LocalDate.now(app.clock)));
        assertEquals(CertificateStatus.EXPIRED, app.generateCertificateDocument.execute(certificate).status());
    }

    @Test
    void expiredCertificateNoLongerPreventsIssuingANewOne() {
        app.issueCertificate.execute(app.closedCompliantInspection(), VALID_UNTIL, app.certifier);
        app.clock.moveTo(VALID_UNTIL.plusDays(1));

        Certificate reissued = app.issueCertificate.execute(app.closedCompliantInspection(),
                VALID_UNTIL.plusYears(1), app.certifier);

        assertEquals("CERT-00002", reissued.number().value());
    }

    @Test
    void suspensionIsRecordedWithItsReasonAndCannotBeRepeated() {
        Certificate certificate = app.issueCertificate.execute(app.closedCompliantInspection(), VALID_UNTIL,
                app.certifier);

        app.suspendCertificate.execute(certificate, "Extinguisher discharged during a drill", app.certifier);

        assertEquals(CertificateStatus.SUSPENDED, certificate.statusOn(LocalDate.now(app.clock)));
        assertEquals("Extinguisher discharged during a drill", certificate.suspensions().getFirst().reason());
        assertEquals(CertificateAudit.SUSPENDED, app.auditService.entriesFor(certificate).getLast().action());
        assertThrows(CertificateNotActiveException.class,
                () -> app.suspendCertificate.execute(certificate, "Again", app.certifier));
    }

    @Test
    void renewalIssuesANewCertificateAndRetiresThePreviousOne() {
        Certificate original = app.issueCertificate.execute(app.closedCompliantInspection(), VALID_UNTIL,
                app.certifier);
        app.clock.moveTo(VALID_UNTIL.minusDays(10));
        Inspection reinspection = app.closedCompliantInspection();

        Certificate renewal = app.renewCertificate.execute(original, reinspection, VALID_UNTIL.plusYears(1),
                app.certifier);

        LocalDate today = LocalDate.now(app.clock);
        assertEquals(CertificateStatus.RENEWED, original.statusOn(today));
        assertEquals(CertificateStatus.ACTIVE, renewal.statusOn(today));
        assertEquals(reinspection, renewal.basedOn());
        assertEquals(renewal, app.certificates.findCurrentFor(app.extinguisher, today).orElseThrow());
        assertEquals(CertificateAudit.RENEWED, app.auditService.entriesFor(original).getLast().action());
        assertEquals(CertificateAudit.ISSUED, app.auditService.entriesFor(renewal).getFirst().action());
        assertThrows(CertificateAlreadyRenewedException.class, () -> app.renewCertificate.execute(original,
                reinspection, VALID_UNTIL.plusYears(2), app.certifier));
    }

    @Test
    void renewalIsSubjectToTheSameEligibilityAsIssuance() {
        Certificate original = app.issueCertificate.execute(app.closedCompliantInspection(), VALID_UNTIL,
                app.certifier);
        Inspection failedReinspection = app.assignedInspection();
        app.majorFindingOf(failedReinspection);

        assertThrows(BlockingFindingsPreventCertificationException.class, () -> app.renewCertificate.execute(original,
                failedReinspection, VALID_UNTIL.plusYears(1), app.certifier));
        assertEquals(CertificateStatus.ACTIVE, original.statusOn(LocalDate.now(app.clock)));
    }
}
