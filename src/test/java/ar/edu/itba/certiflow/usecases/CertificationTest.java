package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateAudit;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certificate.CertificateStatus;
import ar.edu.itba.certiflow.models.certificate.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateAlreadyRenewedException;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateNotActiveException;
import ar.edu.itba.certiflow.models.certificate.exceptions.RenewalRequiresLaterInspectionException;
import ar.edu.itba.certiflow.models.certification.IneligibilityReason;
import ar.edu.itba.certiflow.models.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.models.finding.CorrectiveAction;
import ar.edu.itba.certiflow.models.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.finding.VerificationResult;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CertificationTest {

    private static final LocalDate VALID_UNTIL = CertiflowFixture.TODAY.plusYears(1);

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void assetWithAnOpenBlockingFindingIsNotEligible() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);

        CertiflowFixture.assertNotEligible(IneligibilityReason.BLOCKING_FINDINGS,
                () -> fixture.issueCertificate.execute(inspection, VALID_UNTIL, fixture.certifier));
    }

    @Test
    void inspectionClosedWithoutRaisingItsFindingsIsNotEligible() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "5.00", YesNoAnswer.YES, "VISIBLE");
        fixture.inspection(inspection).close(fixture.inspector, fixture.clock.instant());

        CertiflowFixture.assertNotEligible(IneligibilityReason.UNADDRESSED_NON_CONFORMITIES,
                () -> fixture.issueCertificate.execute(inspection, VALID_UNTIL, fixture.certifier));
    }

    @Test
    void rectificationThatIntroducesABlockingNonConformityRemovesEligibility() {
        InspectionId inspection = fixture.closedCompliantInspection();
        fixture.rectifyInspection.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("5.00"),
                "Gauge reading was transcribed incorrectly: 7.00 instead of 5.00", fixture.certifier);

        CertiflowFixture.assertNotEligible(IneligibilityReason.UNADDRESSED_NON_CONFORMITIES,
                () -> fixture.issueCertificate.execute(inspection, VALID_UNTIL, fixture.certifier));
    }

    @Test
    void openMinorFindingsDoNotPreventCertification() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "7.00", YesNoAnswer.YES, "FADED");
        List<FindingId> findings = fixture.closeInspection.execute(inspection, fixture.inspector);

        CertificateNumber certificate = fixture.issueCertificate.execute(inspection, VALID_UNTIL, fixture.certifier);

        assertTrue(fixture.finding(findings.getFirst()).isOpen());
        assertEquals(CertificateStatus.ACTIVE, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock)));
    }

    @Test
    void assetBecomesEligibleOnceItsBlockingFindingIsClosed() {
        InspectionId inspection = fixture.assignedInspection();
        FindingId finding = fixture.majorFindingOf(inspection);
        CorrectiveActionId rechargeAction = fixture.planCorrectiveAction.execute(finding, "Recharge the extinguisher",
                fixture.maintenanceTechnician, CertiflowFixture.TODAY.plusDays(15), fixture.assetResponsible);
        fixture.verifyCorrectiveAction.execute(finding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED, "Recharged");

        CertificateNumber certificate = fixture.issueCertificate.execute(inspection, VALID_UNTIL, fixture.certifier);

        assertEquals("CERT-00001", fixture.certificate(certificate).number().value());
        assertEquals(fixture.extinguisher, fixture.certificate(certificate).asset());
        assertEquals(inspection, fixture.certificate(certificate).basedOn().id());
        AuditEntry issuanceEntry = fixture.certificateLog.historyOf(fixture.certificate(certificate)).getFirst();
        assertEquals(CertificateAudit.ISSUED, issuanceEntry.action());
        assertEquals(fixture.certifier, issuanceEntry.performedBy());
    }

    @Test
    void inspectionStillInProgressCannotBackACertificate() {
        InspectionId inspectionInProgress = fixture.assignedInspection();

        CertiflowFixture.assertNotEligible(IneligibilityReason.INSPECTION_NOT_CLOSED,
                () -> fixture.issueCertificate.execute(inspectionInProgress, VALID_UNTIL, fixture.certifier));
    }

    @Test
    void assetHoldsASingleCurrentCertificate() {
        InspectionId firstInspection = fixture.closedCompliantInspection();
        fixture.issueCertificate.execute(firstInspection, VALID_UNTIL, fixture.certifier);
        InspectionId secondInspection = fixture.closedCompliantInspection();

        assertThrows(AssetAlreadyCertifiedException.class,
                () -> fixture.issueCertificate.execute(secondInspection, VALID_UNTIL, fixture.certifier));
    }

    @Test
    void suspendedCertificateStillOccupiesTheAssetSoANewOneCannotBeIssuedAroundIt() {
        CertificateNumber certificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);
        fixture.suspendCertificate.execute(certificate, "Extinguisher discharged during a drill", fixture.certifier);

        assertThrows(AssetAlreadyCertifiedException.class, () -> fixture.issueCertificate.execute(
                fixture.closedCompliantInspection(), VALID_UNTIL, fixture.certifier));
    }

    @Test
    void certificateIsActiveThroughItsLastDayAndExpiredTheDayAfter() {
        CertificateNumber certificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);

        fixture.clock.moveTo(VALID_UNTIL);
        assertEquals(CertificateStatus.ACTIVE, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock)));
        assertEquals(CertificateStatus.ACTIVE, fixture.generateCertificateDocument.execute(certificate).status());

        fixture.clock.advanceDays(1);
        assertEquals(CertificateStatus.EXPIRED, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock)));
        assertEquals(CertificateStatus.EXPIRED, fixture.generateCertificateDocument.execute(certificate).status());
    }

    @Test
    void expiredCertificateNoLongerPreventsIssuingANewOne() {
        fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL, fixture.certifier);
        fixture.clock.moveTo(VALID_UNTIL.plusDays(1));

        CertificateNumber reissuedCertificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(),
                VALID_UNTIL.plusYears(1), fixture.certifier);

        assertEquals("CERT-00002", fixture.certificate(reissuedCertificate).number().value());
    }

    @Test
    void suspensionIsRecordedWithItsReasonAndCannotBeRepeated() {
        CertificateNumber certificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);

        fixture.suspendCertificate.execute(certificate, "Extinguisher discharged during a drill", fixture.certifier);

        assertEquals(CertificateStatus.SUSPENDED, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock)));
        assertEquals("Extinguisher discharged during a drill", fixture.certificate(certificate).suspensions().getFirst().reason());
        assertEquals(CertificateAudit.SUSPENDED, fixture.certificateLog.historyOf(fixture.certificate(certificate)).getLast().action());
        assertThrows(CertificateNotActiveException.class,
                () -> fixture.suspendCertificate.execute(certificate, "Again", fixture.certifier));
    }

    @Test
    void renewalIssuesANewCertificateAndRetiresThePreviousOne() {
        CertificateNumber originalCertificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);
        fixture.clock.moveTo(VALID_UNTIL.minusDays(10));
        InspectionId reinspection = fixture.closedCompliantInspection();

        CertificateNumber renewedCertificate = fixture.renewCertificate.execute(originalCertificate, reinspection, VALID_UNTIL.plusYears(1),
                fixture.certifier);

        LocalDate currentDate = LocalDate.now(fixture.clock);
        assertEquals(CertificateStatus.RENEWED, fixture.certificate(originalCertificate).statusOn(currentDate));
        assertEquals(CertificateStatus.ACTIVE, fixture.certificate(renewedCertificate).statusOn(currentDate));
        assertEquals(reinspection, fixture.certificate(renewedCertificate).basedOn().id());
        assertEquals(renewedCertificate, fixture.certificateRepository.findCurrentFor(fixture.extinguisher, currentDate).orElseThrow().number());
        assertEquals(CertificateAudit.RENEWED, fixture.certificateLog.historyOf(fixture.certificate(originalCertificate)).getLast().action());
        assertEquals(CertificateAudit.ISSUED, fixture.certificateLog.historyOf(fixture.certificate(renewedCertificate)).getFirst().action());
        assertThrows(CertificateAlreadyRenewedException.class, () -> fixture.renewCertificate.execute(originalCertificate,
                reinspection, VALID_UNTIL.plusYears(2), fixture.certifier));
    }

    @Test
    void expiredCertificateCannotBeSuspended() {
        CertificateNumber certificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);
        fixture.clock.moveTo(VALID_UNTIL.plusDays(1));

        assertThrows(CertificateNotActiveException.class,
                () -> fixture.suspendCertificate.execute(certificate, "Discharged", fixture.certifier));
        assertTrue(fixture.certificate(certificate).suspensions().isEmpty());
        assertEquals(CertificateStatus.EXPIRED, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock)));
    }

    @Test
    void renewalRequiresAnInspectionClosedAfterTheOneBehindTheCurrentCertificate() {
        InspectionId earlierInspection = fixture.closedCompliantInspection();
        fixture.clock.advanceDays(1);
        InspectionId certifiedInspection = fixture.closedCompliantInspection();
        CertificateNumber certificate = fixture.issueCertificate.execute(certifiedInspection, VALID_UNTIL, fixture.certifier);

        assertThrows(RenewalRequiresLaterInspectionException.class, () -> fixture.renewCertificate.execute(certificate,
                certifiedInspection, VALID_UNTIL.plusYears(1), fixture.certifier));
        assertThrows(RenewalRequiresLaterInspectionException.class, () -> fixture.renewCertificate.execute(certificate,
                earlierInspection, VALID_UNTIL.plusYears(1), fixture.certifier));
        assertEquals(CertificateStatus.ACTIVE, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock)));
    }

    @Test
    void failedRenewalDoesNotConsumeACertificateNumber() {
        InspectionId inspection = fixture.closedCompliantInspection();
        CertificateNumber certificate = fixture.issueCertificate.execute(inspection, VALID_UNTIL, fixture.certifier);
        assertThrows(RenewalRequiresLaterInspectionException.class, () -> fixture.renewCertificate.execute(certificate,
                inspection, VALID_UNTIL.plusYears(1), fixture.certifier));
        fixture.clock.advanceDays(1);

        CertificateNumber renewedCertificate = fixture.renewCertificate.execute(certificate, fixture.closedCompliantInspection(),
                VALID_UNTIL.plusYears(1), fixture.certifier);

        assertEquals("CERT-00002", fixture.certificate(renewedCertificate).number().value());
    }

    @Test
    void renewalIsSubjectToTheSameEligibilityAsIssuance() {
        CertificateNumber originalCertificate = fixture.issueCertificate.execute(fixture.closedCompliantInspection(), VALID_UNTIL,
                fixture.certifier);
        InspectionId failedReinspection = fixture.assignedInspection();
        fixture.majorFindingOf(failedReinspection);

        CertiflowFixture.assertNotEligible(IneligibilityReason.BLOCKING_FINDINGS, () -> fixture.renewCertificate.execute(originalCertificate,
                failedReinspection, VALID_UNTIL.plusYears(1), fixture.certifier));
        assertEquals(CertificateStatus.ACTIVE, fixture.certificate(originalCertificate).statusOn(LocalDate.now(fixture.clock)));
    }
}
