package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.application.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.application.exceptions.BlockingFindingsPreventCertificationException;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateAudit;
import ar.edu.itba.certiflow.models.certificate.CertificateStatus;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateAlreadyRenewedException;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateNotActiveException;
import ar.edu.itba.certiflow.models.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.models.finding.CorrectiveAction;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.VerificationResult;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CertificationTest {

    private static final LocalDate VALID_UNTIL = CertiflowFixture.TODAY.plusYears(1);

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void assetWithAnOpenBlockingFindingIsNotEligible() {
        Inspection inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);

        assertThrows(BlockingFindingsPreventCertificationException.class,
                () -> fixture.issueCertificate.execute(inspection, VALID_UNTIL, fixture.certifier));
    }

    @Test
    void openMinorFindingsDoNotPreventCertification() {
        Inspection inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "7.00", YesNoAnswer.YES, "FADED");
        List<Finding> findings = fixture.closeInspection.execute(inspection, fixture.inspector);

        Certificate certificate = fixture.issueCertificate.execute(inspection, VALID_UNTIL, fixture.certifier);

        assertTrue(findings.getFirst().isOpen());
        assertEquals(CertificateStatus.ACTIVE, certificate.statusOn(LocalDate.now(fixture.clock)));
    }

    @Test
    void assetBecomesEligibleOnceItsBlockingFindingIsClosed() {
        Inspection inspection = fixture.assignedInspection();
        Finding finding = fixture.majorFindingOf(inspection);
        CorrectiveAction rechargeAction = fixture.planCorrectiveAction.execute(finding, "Recharge the extinguisher",
                fixture.maintenanceTechnician, CertiflowFixture.TODAY.plusDays(15), fixture.assetResponsible);
        fixture.verifyCorrectiveAction.execute(finding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED, "Recharged");

        Certificate certificate = fixture.issueCertificate.execute(inspection, VALID_UNTIL, fixture.certifier);

        assertEquals("CERT-00001", certificate.number().value());
        assertEquals(fixture.extinguisher, certificate.asset());
        assertEquals(inspection, certificate.basedOn());
        AuditEntry issuanceEntry = fixture.certificateLog.historyOf(certificate).getFirst();
        assertEquals(CertificateAudit.ISSUED, issuanceEntry.action());
        assertEquals(fixture.certifier, issuanceEntry.performedBy());
    }

    @Test
    void inspectionStillInProgressCannotBackACertificate() {
        Inspection inspectionInProgress = fixture.assignedInspection();

        assertThrows(InspectionNotClosedException.class,
                () -> fixture.issueCertificate.execute(inspectionInProgress, VALID_UNTIL, fixture.certifier));
    }

    @Test
    void assetHoldsASingleCurrentCertificate() {
        Inspection firstInspection = fixture.closedCompliantInspection();
        fixture.issueCertificate.execute(firstInspection, VALID_UNTIL, fixture.certifier);
        Inspection secondInspection = fixture.closedCompliantInspection();

        assertThrows(AssetAlreadyCertifiedException.class,
                () -> fixture.issueCertificate.execute(secondInspection, VALID_UNTIL, fixture.certifier));
    }

    @Test
    void suspendedCertificateStillOccupiesTheAssetSoANewOneCannotBeIssuedAroundIt() {
        Certificate certificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);
        fixture.suspendCertificate.execute(certificate, "Extinguisher discharged during a drill", fixture.certifier);

        assertThrows(AssetAlreadyCertifiedException.class, () -> fixture.issueCertificate.execute(
                fixture.closedCompliantInspection(), VALID_UNTIL, fixture.certifier));
    }

    @Test
    void certificateIsActiveThroughItsLastDayAndExpiredTheDayAfter() {
        Certificate certificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);

        fixture.clock.moveTo(VALID_UNTIL);
        assertEquals(CertificateStatus.ACTIVE, certificate.statusOn(LocalDate.now(fixture.clock)));
        assertEquals(CertificateStatus.ACTIVE, fixture.generateCertificateDocument.execute(certificate).status());

        fixture.clock.advanceDays(1);
        assertEquals(CertificateStatus.EXPIRED, certificate.statusOn(LocalDate.now(fixture.clock)));
        assertEquals(CertificateStatus.EXPIRED, fixture.generateCertificateDocument.execute(certificate).status());
    }

    @Test
    void expiredCertificateNoLongerPreventsIssuingANewOne() {
        fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL, fixture.certifier);
        fixture.clock.moveTo(VALID_UNTIL.plusDays(1));

        Certificate reissuedCertificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(),
                VALID_UNTIL.plusYears(1), fixture.certifier);

        assertEquals("CERT-00002", reissuedCertificate.number().value());
    }

    @Test
    void suspensionIsRecordedWithItsReasonAndCannotBeRepeated() {
        Certificate certificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);

        fixture.suspendCertificate.execute(certificate, "Extinguisher discharged during a drill", fixture.certifier);

        assertEquals(CertificateStatus.SUSPENDED, certificate.statusOn(LocalDate.now(fixture.clock)));
        assertEquals("Extinguisher discharged during a drill", certificate.suspensions().getFirst().reason());
        assertEquals(CertificateAudit.SUSPENDED, fixture.certificateLog.historyOf(certificate).getLast().action());
        assertThrows(CertificateNotActiveException.class,
                () -> fixture.suspendCertificate.execute(certificate, "Again", fixture.certifier));
    }

    @Test
    void renewalIssuesANewCertificateAndRetiresThePreviousOne() {
        Certificate originalCertificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);
        fixture.clock.moveTo(VALID_UNTIL.minusDays(10));
        Inspection reinspection = fixture.closedCompliantInspection();

        Certificate renewedCertificate = fixture.renewCertificate.execute(originalCertificate, reinspection, VALID_UNTIL.plusYears(1),
                fixture.certifier);

        LocalDate currentDate = LocalDate.now(fixture.clock);
        assertEquals(CertificateStatus.RENEWED, originalCertificate.statusOn(currentDate));
        assertEquals(CertificateStatus.ACTIVE, renewedCertificate.statusOn(currentDate));
        assertEquals(reinspection, renewedCertificate.basedOn());
        assertEquals(renewedCertificate, fixture.certificateRepository.findCurrentFor(fixture.extinguisher, currentDate).orElseThrow());
        assertEquals(CertificateAudit.RENEWED, fixture.certificateLog.historyOf(originalCertificate).getLast().action());
        assertEquals(CertificateAudit.ISSUED, fixture.certificateLog.historyOf(renewedCertificate).getFirst().action());
        assertThrows(CertificateAlreadyRenewedException.class, () -> fixture.renewCertificate.execute(originalCertificate,
                reinspection, VALID_UNTIL.plusYears(2), fixture.certifier));
    }

    @Test
    void renewalIsSubjectToTheSameEligibilityAsIssuance() {
        Certificate originalCertificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);
        Inspection failedReinspection = fixture.assignedInspection();
        fixture.majorFindingOf(failedReinspection);

        assertThrows(BlockingFindingsPreventCertificationException.class, () -> fixture.renewCertificate.execute(originalCertificate,
                failedReinspection, VALID_UNTIL.plusYears(1), fixture.certifier));
        assertEquals(CertificateStatus.ACTIVE, originalCertificate.statusOn(LocalDate.now(fixture.clock)));
    }
}
