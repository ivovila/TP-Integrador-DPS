package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.CertificateAudit;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateStatus;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.CertificationConflictException;
import ar.edu.itba.certiflow.certification.model.SubsystemNotFoundException;
import ar.edu.itba.certiflow.certification.model.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateAlreadyRenewedException;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotActiveException;
import ar.edu.itba.certiflow.certification.model.exceptions.RenewalRequiresLaterInspectionException;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateCommand;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ar.edu.itba.certiflow.certification.model.StandardIneligibilityReason;

class CertificationTest {

    private static final LocalDate VALID_UNTIL = CertiflowFixture.TODAY.plusYears(1).minusDays(1);

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void assetWithAnOpenBlockingFindingIsNotEligible() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);

        CertiflowFixture.assertNotEligible(StandardIneligibilityReason.BLOCKING_FINDINGS,
                () -> fixture.issue(inspection, fixture.certifier));
    }

    @Test
    void rectificationThatIntroducesABlockingNonConformityRaisesItsFindingAndRemovesEligibility() {
        InspectionId inspection = fixture.closedCompliantInspection();

        List<FindingId> raisedFindings = fixture.rectify(inspection, fixture.pressureCriterion, fixture.numericAnswer("5.00"), "Gauge reading was transcribed incorrectly: 7.00 instead of 5.00", fixture.certifier);

        assertEquals(1, raisedFindings.size());
        assertTrue(fixture.finding(raisedFindings.getFirst()).isOpen());
        CertiflowFixture.assertNotEligible(StandardIneligibilityReason.BLOCKING_FINDINGS,
                () -> fixture.issue(inspection, fixture.certifier));
    }

    @Test
    void aClosedFindingDoesNotCoverANonConformityReintroducedByARectification() {
        InspectionId inspection = fixture.assignedInspection();
        FindingId finding = fixture.majorFindingOf(inspection);
        fixture.closeFinding(finding);

        fixture.rectify(inspection, fixture.pressureCriterion, fixture.numericAnswer("4.00"), "Gauge reading was transcribed incorrectly: 5.00 instead of 4.00", fixture.certifier);

        CertiflowFixture.assertNotEligible(StandardIneligibilityReason.BLOCKING_FINDINGS,
                () -> fixture.issue(inspection, fixture.certifier));
    }

    @Test
    void aRectificationThatFixesTheAnswerRaisesNoFinding() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);

        List<FindingId> raisedFindings = fixture.rectify(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), "Gauge reading was transcribed incorrectly: 5.00 instead of 7.00", fixture.certifier);

        assertTrue(raisedFindings.isEmpty());
    }

    @Test
    void openMinorFindingsDoNotPreventCertification() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "7.00", YesNoAnswer.YES, "FADED");
        List<FindingId> findings = fixture.close(inspection, fixture.inspector);

        CertificateNumber certificate = fixture.issue(inspection, fixture.certifier);

        assertTrue(fixture.finding(findings.getFirst()).isOpen());
        assertEquals(CertificateStatus.ACTIVE, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock), List.of()));
    }

    @Test
    void assetBecomesEligibleOnceItsBlockingFindingIsClosed() {
        InspectionId inspection = fixture.assignedInspection();
        FindingId finding = fixture.majorFindingOf(inspection);
        CorrectiveActionId rechargeAction = fixture.plan(finding, "Recharge the extinguisher", fixture.maintenanceTechnician, CertiflowFixture.TODAY.plusDays(15), fixture.assetResponsible);
        fixture.verify(finding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED, "Recharged");

        CertificateNumber certificate = fixture.issue(inspection, fixture.certifier);

        assertEquals("CERT-00001", fixture.certificate(certificate).number().value());
        assertEquals(fixture.extinguisher.code(), fixture.certificate(certificate).assetCode());
        assertEquals(inspection, fixture.certificate(certificate).basedOn().id());
        AuditEntry issuanceEntry = fixture.certificateLog.historyOf(certificate).getFirst();
        assertEquals(CertificateAudit.ISSUED, issuanceEntry.action());
        assertEquals(fixture.certifier, issuanceEntry.performedBy());
    }

    @Test
    void inspectionStillInProgressCannotBackACertificate() {
        InspectionId inspectionInProgress = fixture.assignedInspection();

        CertiflowFixture.assertNotEligible(StandardIneligibilityReason.INSPECTION_NOT_CLOSED,
                () -> fixture.issue(inspectionInProgress, fixture.certifier));
    }

    @Test
    void assetHoldsASingleCurrentCertificate() {
        InspectionId firstInspection = fixture.closedCompliantInspection();
        fixture.issue(firstInspection, fixture.certifier);
        InspectionId secondInspection = fixture.closedCompliantInspection();

        assertThrows(AssetAlreadyCertifiedException.class,
                () -> fixture.issue(secondInspection, fixture.certifier));
    }

    @Test
    void suspendedCertificateStillOccupiesTheAssetSoANewOneCannotBeIssuedAroundIt() {
        CertificateNumber certificate = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);
        fixture.suspend(certificate, "Extinguisher discharged during a drill", fixture.certifier);

        assertThrows(AssetAlreadyCertifiedException.class, () -> fixture.issue(fixture.closedCompliantInspection(), fixture.certifier));
    }

    @ParameterizedTest(name = "{0} days after the last valid day: {1}")
    @CsvSource({"-1,ACTIVE", "0,ACTIVE", "1,EXPIRED"})
    void certificateIsActiveThroughItsLastDayAndExpiredTheDayAfter(int daysAfterValidUntil, CertificateStatus expected) {
        CertificateNumber certificate = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);

        fixture.clock.moveTo(VALID_UNTIL.plusDays(daysAfterValidUntil));

        assertEquals(expected, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock), List.of()));
        assertEquals(expected, fixture.generateCertificateDocument.execute(certificate).status());
    }

    @Test
    void expiredCertificateNoLongerPreventsIssuingANewOne() {
        fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);
        fixture.clock.moveTo(VALID_UNTIL.plusDays(1));

        CertificateNumber reissuedCertificate = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);

        assertEquals("CERT-00002", fixture.certificate(reissuedCertificate).number().value());
    }

    @Test
    void suspensionIsRecordedWithItsReasonAndCannotBeRepeated() {
        CertificateNumber certificate = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);

        fixture.suspend(certificate, "Extinguisher discharged during a drill", fixture.certifier);

        assertEquals(CertificateStatus.SUSPENDED, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock), List.of()));
        assertEquals("Extinguisher discharged during a drill", fixture.certificate(certificate).suspensions().getFirst().reason());
        assertEquals(CertificateAudit.SUSPENDED, fixture.certificateLog.historyOf(certificate).getLast().action());
        assertThrows(CertificateNotActiveException.class,
                () -> fixture.suspend(certificate, "Again", fixture.certifier));
    }

    @Test
    void renewalIssuesANewCertificateAndRetiresThePreviousOne() {
        CertificateNumber originalCertificate = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);
        fixture.clock.moveTo(VALID_UNTIL.minusDays(10));
        InspectionId reinspection = fixture.closedCompliantInspection();

        CertificateNumber renewedCertificate = fixture.renew(originalCertificate, reinspection, fixture.certifier);

        LocalDate currentDate = LocalDate.now(fixture.clock);
        assertEquals(CertificateStatus.RENEWED, fixture.certificate(originalCertificate).statusOn(currentDate, List.of()));
        assertEquals(CertificateStatus.ACTIVE, fixture.certificate(renewedCertificate).statusOn(currentDate, List.of()));
        assertEquals(reinspection, fixture.certificate(renewedCertificate).basedOn().id());
        assertEquals(renewedCertificate, fixture.certificateRepository.findCurrentFor(fixture.extinguisher.code(), currentDate).orElseThrow().number());
        assertEquals(CertificateAudit.RENEWED, fixture.certificateLog.historyOf(originalCertificate).getLast().action());
        assertEquals(CertificateAudit.ISSUED, fixture.certificateLog.historyOf(renewedCertificate).getFirst().action());
        assertThrows(CertificateAlreadyRenewedException.class, () -> fixture.renew(originalCertificate, reinspection, fixture.certifier));
    }

    @Test
    void expiredCertificateCannotBeSuspended() {
        CertificateNumber certificate = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);
        fixture.clock.moveTo(VALID_UNTIL.plusDays(1));

        assertThrows(CertificateNotActiveException.class,
                () -> fixture.suspend(certificate, "Discharged", fixture.certifier));
        assertTrue(fixture.certificate(certificate).suspensions().isEmpty());
        assertEquals(CertificateStatus.EXPIRED, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock), List.of()));
    }

    @Test
    void renewalRequiresAnInspectionClosedAfterTheOneBehindTheCurrentCertificate() {
        InspectionId earlierInspection = fixture.closedCompliantInspection();
        fixture.clock.advanceDays(1);
        InspectionId certifiedInspection = fixture.closedCompliantInspection();
        CertificateNumber certificate = fixture.issue(certifiedInspection, fixture.certifier);

        assertThrows(RenewalRequiresLaterInspectionException.class, () -> fixture.renew(certificate, certifiedInspection, fixture.certifier));
        assertThrows(RenewalRequiresLaterInspectionException.class, () -> fixture.renew(certificate, earlierInspection, fixture.certifier));
        assertEquals(CertificateStatus.ACTIVE, fixture.certificate(certificate).statusOn(LocalDate.now(fixture.clock), List.of()));
    }

    @Test
    void failedRenewalDoesNotConsumeACertificateNumber() {
        InspectionId inspection = fixture.closedCompliantInspection();
        CertificateNumber certificate = fixture.issue(inspection, fixture.certifier);
        assertThrows(RenewalRequiresLaterInspectionException.class, () -> fixture.renew(certificate, inspection, fixture.certifier));
        fixture.clock.advanceDays(1);

        CertificateNumber renewedCertificate = fixture.renew(certificate, fixture.closedCompliantInspection(), fixture.certifier);

        assertEquals("CERT-00002", fixture.certificate(renewedCertificate).number().value());
    }

    @Test
    void anUnknownSubsystemConsumesNoCertificateNumber() {
        var inspection = fixture.closedCompliantInspection();
        assertThrows(SubsystemNotFoundException.class,
                () -> fixture.issueCertificate.execute(new IssueCertificateCommand(
                        inspection, fixture.certifier, new CertificationScope.SubsystemScope(
                                new SubsystemCode("MISSING")))));
        assertEquals("CERT-00001", fixture.issue(inspection, fixture.certifier).value());
    }

    @Test
    void aRenewalForAnotherScopeIsRejectedAndConsumesNoCertificateNumber() {
        var number = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);
        fixture.clock.advanceDays(1);
        var inspection = fixture.closedCompliantInspection();
        assertThrows(CertificationConflictException.class,
                () -> fixture.renewCertificate.execute(new RenewCertificateCommand(
                        number, inspection, fixture.certifier, new CertificationScope.SubsystemScope(
                                new SubsystemCode("MISSING")))));
        assertEquals("CERT-00002", fixture.renew(number, inspection, fixture.certifier).value());
    }

    @Test
    void aSuspendedCertificateIsExpiredOnceItsValidityIsOver() {
        CertificateNumber certificate = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);
        fixture.suspend(certificate, "Discharged after a fire", fixture.certifier);
        assertEquals(CertificateStatus.SUSPENDED, fixture.certificate(certificate).statusOn(CertiflowFixture.TODAY, List.of()));

        assertEquals(CertificateStatus.EXPIRED, fixture.certificate(certificate).statusOn(VALID_UNTIL.plusDays(1), List.of()));
    }

    @Test
    void renewalIsSubjectToTheSameEligibilityAsIssuance() {
        CertificateNumber originalCertificate = fixture.issue(fixture.closedCompliantInspection(), fixture.certifier);
        InspectionId failedReinspection = fixture.assignedInspection();
        fixture.majorFindingOf(failedReinspection);

        CertiflowFixture.assertNotEligible(StandardIneligibilityReason.BLOCKING_FINDINGS, () -> fixture.renew(originalCertificate, failedReinspection, fixture.certifier));
        assertEquals(CertificateStatus.ACTIVE, fixture.certificate(originalCertificate).statusOn(LocalDate.now(fixture.clock), List.of()));
    }
}
