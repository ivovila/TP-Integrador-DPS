package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.models.finding.CorrectiveAction;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingAudit;
import ar.edu.itba.certiflow.models.finding.VerificationResult;
import ar.edu.itba.certiflow.models.finding.exceptions.CorrectiveActionNotInFindingException;
import ar.edu.itba.certiflow.models.finding.exceptions.FindingAlreadyClosedException;
import ar.edu.itba.certiflow.models.finding.exceptions.VerifierMustDifferFromResponsibleException;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CorrectiveActionTest {

    private static final LocalDate DUE_DATE = CertiflowFixture.TODAY.plusDays(30);

    private final CertiflowFixture fixture = new CertiflowFixture();
    private Finding finding;
    private CorrectiveAction rechargeAction;

    @BeforeEach
    void planTheRecharge() {
        finding = fixture.majorFindingOf(fixture.assignedInspection());
        rechargeAction = fixture.planCorrectiveAction.execute(finding, "Recharge the extinguisher", fixture.maintenanceTechnician, DUE_DATE,
                fixture.assetResponsible);
    }

    @Test
    void actionIsNotOverdueOnItsDueDateButIsTheDayAfter() {
        fixture.clock.moveTo(DUE_DATE);
        assertFalse(rechargeAction.isOverdueOn(LocalDate.now(fixture.clock)));
        assertEquals(0, fixture.generateFindingsSummary.execute(fixture.extinguisher).findings().getFirst().overdueActions());

        fixture.clock.advanceDays(1);

        assertTrue(rechargeAction.isOverdueOn(LocalDate.now(fixture.clock)));
        assertEquals(1, fixture.generateFindingsSummary.execute(fixture.extinguisher).findings().getFirst().overdueActions());
    }

    @Test
    void rejectedVerificationKeepsActionAndFindingOpenAndIsAudited() {
        fixture.verifyCorrectiveAction.execute(finding, rechargeAction, fixture.verifier, VerificationResult.REJECTED,
                "Gauge still reads 5 bar");

        assertFalse(rechargeAction.isClosed());
        assertTrue(finding.isOpen());
        assertEquals(1, rechargeAction.verifications().size());
        assertEquals(FindingAudit.VERIFICATION_REJECTED, fixture.auditService.entriesFor(finding).getLast().action());
        assertEquals(fixture.verifier, fixture.auditService.entriesFor(finding).getLast().performedBy());
    }

    @Test
    void acceptedVerificationClosesTheActionAndItsFindingAndTheActionStopsBeingOverdue() {
        fixture.clock.moveTo(DUE_DATE.plusDays(5));
        assertTrue(rechargeAction.isOverdueOn(LocalDate.now(fixture.clock)));

        fixture.verifyCorrectiveAction.execute(finding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED,
                "Recharged, gauge reads 7 bar");

        assertTrue(rechargeAction.isClosed());
        assertFalse(finding.isOpen());
        assertFalse(finding.blocksCertification());
        assertFalse(rechargeAction.isOverdueOn(LocalDate.now(fixture.clock)));
        assertEquals(FindingAudit.VERIFICATION_ACCEPTED, fixture.auditService.entriesFor(finding).getLast().action());
    }

    @Test
    void whoeverIsResponsibleForTheActionCannotVerifyIt() {
        assertThrows(VerifierMustDifferFromResponsibleException.class, () -> fixture.verifyCorrectiveAction.execute(
                finding, rechargeAction, fixture.maintenanceTechnician, VerificationResult.ACCEPTED, "Looks fine to me"));

        assertTrue(rechargeAction.verifications().isEmpty());
        assertTrue(finding.isOpen());
    }

    @Test
    void closedFindingAcceptsNoFurtherActionsNorVerifications() {
        fixture.verifyCorrectiveAction.execute(finding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED, "Done");

        assertThrows(FindingAlreadyClosedException.class, () -> fixture.planCorrectiveAction.execute(finding,
                "Replace the unit", fixture.maintenanceTechnician, DUE_DATE, fixture.assetResponsible));
        assertThrows(FindingAlreadyClosedException.class, () -> fixture.verifyCorrectiveAction.execute(finding, rechargeAction,
                fixture.verifier, VerificationResult.REJECTED, "Second opinion"));
    }

    @Test
    void actionCanOnlyBeVerifiedThroughTheFindingItBelongsTo() {
        Finding anotherFinding = fixture.majorFindingOf(fixture.assignedInspection());

        assertThrows(CorrectiveActionNotInFindingException.class, () -> fixture.verifyCorrectiveAction.execute(anotherFinding,
                rechargeAction, fixture.verifier, VerificationResult.ACCEPTED, "Wrong finding"));
    }

    @Test
    void planningIsAuditedAndAttributedToWhoeverPlanned() {
        assertEquals(FindingAudit.ACTION_PLANNED, fixture.auditService.entriesFor(finding).getLast().action());
        assertEquals(fixture.assetResponsible, fixture.auditService.entriesFor(finding).getLast().performedBy());
    }

    @Test
    void actionRemembersWhoPlannedItAndWhen() {
        assertEquals(fixture.assetResponsible, rechargeAction.plannedBy());
        assertEquals(fixture.clock.instant(), rechargeAction.plannedAt());
    }
}
