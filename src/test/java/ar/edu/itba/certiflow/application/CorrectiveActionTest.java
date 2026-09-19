package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.domain.finding.CorrectiveAction;
import ar.edu.itba.certiflow.domain.finding.exceptions.CorrectiveActionNotInFindingException;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.finding.exceptions.FindingAlreadyClosedException;
import ar.edu.itba.certiflow.domain.finding.FindingAudit;
import ar.edu.itba.certiflow.domain.finding.VerificationResult;
import ar.edu.itba.certiflow.domain.finding.exceptions.VerifierMustDifferFromResponsibleException;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CorrectiveActionTest {

    private static final LocalDate DUE_DATE = CertiflowFixture.TODAY.plusDays(30);

    private final CertiflowFixture app = new CertiflowFixture();
    private Finding finding;
    private CorrectiveAction recharge;

    @BeforeEach
    void planTheRecharge() {
        finding = app.majorFindingOf(app.assignedInspection());
        recharge = app.planCorrectiveAction.execute(finding, "Recharge the extinguisher", app.maintenance, DUE_DATE,
                app.assetResponsible);
    }

    @Test
    void actionIsNotOverdueOnItsDueDateButIsTheDayAfter() {
        app.clock.moveTo(DUE_DATE);
        assertFalse(recharge.isOverdueOn(LocalDate.now(app.clock)));
        assertEquals(0, app.generateFindingsSummary.execute(app.extinguisher).findings().getFirst().overdueActions());

        app.clock.advanceDays(1);

        assertTrue(recharge.isOverdueOn(LocalDate.now(app.clock)));
        assertEquals(1, app.generateFindingsSummary.execute(app.extinguisher).findings().getFirst().overdueActions());
    }

    @Test
    void rejectedVerificationKeepsActionAndFindingOpenAndIsAudited() {
        app.verifyCorrectiveAction.execute(finding, recharge, app.verifier, VerificationResult.REJECTED,
                "Gauge still reads 5 bar");

        assertFalse(recharge.isClosed());
        assertTrue(finding.isOpen());
        assertEquals(1, recharge.verifications().size());
        assertEquals(FindingAudit.VERIFICATION_REJECTED, app.auditService.entriesFor(finding).getLast().action());
        assertEquals(app.verifier, app.auditService.entriesFor(finding).getLast().by());
    }

    @Test
    void acceptedVerificationClosesTheActionAndItsFindingAndTheActionStopsBeingOverdue() {
        app.clock.moveTo(DUE_DATE.plusDays(5));
        assertTrue(recharge.isOverdueOn(LocalDate.now(app.clock)));

        app.verifyCorrectiveAction.execute(finding, recharge, app.verifier, VerificationResult.ACCEPTED,
                "Recharged, gauge reads 7 bar");

        assertTrue(recharge.isClosed());
        assertFalse(finding.isOpen());
        assertFalse(finding.blocksCertification());
        assertFalse(recharge.isOverdueOn(LocalDate.now(app.clock)));
        assertEquals(FindingAudit.VERIFICATION_ACCEPTED, app.auditService.entriesFor(finding).getLast().action());
    }

    @Test
    void whoeverIsResponsibleForTheActionCannotVerifyIt() {
        assertThrows(VerifierMustDifferFromResponsibleException.class, () -> app.verifyCorrectiveAction.execute(
                finding, recharge, app.maintenance, VerificationResult.ACCEPTED, "Looks fine to me"));

        assertTrue(recharge.verifications().isEmpty());
        assertTrue(finding.isOpen());
    }

    @Test
    void closedFindingAcceptsNoFurtherActionsNorVerifications() {
        app.verifyCorrectiveAction.execute(finding, recharge, app.verifier, VerificationResult.ACCEPTED, "Done");

        assertThrows(FindingAlreadyClosedException.class, () -> app.planCorrectiveAction.execute(finding,
                "Replace the unit", app.maintenance, DUE_DATE, app.assetResponsible));
        assertThrows(FindingAlreadyClosedException.class, () -> app.verifyCorrectiveAction.execute(finding, recharge,
                app.verifier, VerificationResult.REJECTED, "Second opinion"));
    }

    @Test
    void actionCanOnlyBeVerifiedThroughTheFindingItBelongsTo() {
        Finding another = app.majorFindingOf(app.assignedInspection());

        assertThrows(CorrectiveActionNotInFindingException.class, () -> app.verifyCorrectiveAction.execute(another,
                recharge, app.verifier, VerificationResult.ACCEPTED, "Wrong finding"));
    }

    @Test
    void planningIsAuditedAndAttributedToWhoeverPlanned() {
        assertEquals(FindingAudit.ACTION_PLANNED, app.auditService.entriesFor(finding).getLast().action());
        assertEquals(app.assetResponsible, app.auditService.entriesFor(finding).getLast().by());
    }

    @Test
    void actionRemembersWhoPlannedItAndWhen() {
        assertEquals(app.assetResponsible, recharge.plannedBy());
        assertEquals(app.clock.instant(), recharge.plannedAt());
    }
}
