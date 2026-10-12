package ar.edu.itba.certiflow.application;

import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.ParameterizedTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.FindingAudit;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.VerificationResult;
import ar.edu.itba.certiflow.inspection.model.exceptions.CorrectiveActionNotInFindingException;
import ar.edu.itba.certiflow.inspection.model.exceptions.DueDateInThePastException;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingAlreadyClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.VerificationNotesRequiredException;
import ar.edu.itba.certiflow.inspection.model.exceptions.VerifierMustDifferFromResponsibleException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CorrectiveActionTest {

    private static final LocalDate DUE_DATE = CertiflowFixture.TODAY.plusDays(30);

    private final CertiflowFixture fixture = new CertiflowFixture();
    private FindingId finding;
    private CorrectiveActionId rechargeAction;

    @BeforeEach
    void planTheRecharge() {
        finding = fixture.majorFindingOf(fixture.assignedInspection());
        rechargeAction = fixture.plan(finding, "Recharge the extinguisher", fixture.maintenanceTechnician, DUE_DATE, fixture.assetResponsible);
    }

    @ParameterizedTest(name = "{0} days after the due date: overdue={1}")
    @CsvSource({"-1,false", "0,false", "1,true", "30,true"})
    void actionIsOverdueOnlyAfterItsDueDate(int daysAfterDueDate, boolean overdue) {
        fixture.clock.moveTo(DUE_DATE.plusDays(daysAfterDueDate));

        assertEquals(overdue, fixture.action(finding, rechargeAction).isOverdueOn(LocalDate.now(fixture.clock)));
        assertEquals(overdue ? 1 : 0,
                fixture.generateFindingsSummary.execute(fixture.extinguisher.code()).findings().getFirst().overdueActions());
    }

    @Test
    void rejectedVerificationKeepsActionAndFindingOpenAndIsAudited() {
        fixture.verify(finding, rechargeAction, fixture.verifier, VerificationResult.REJECTED, "Gauge still reads 5 bar");

        assertFalse(fixture.action(finding, rechargeAction).isClosed());
        assertTrue(fixture.finding(finding).isOpen());
        assertEquals(1, fixture.action(finding, rechargeAction).verifications().size());
        assertEquals(FindingAudit.VERIFICATION_REJECTED, fixture.findingLog.historyOf(finding).getLast().action());
        assertEquals(fixture.verifier, fixture.findingLog.historyOf(finding).getLast().performedBy());
    }

    @Test
    void acceptedVerificationClosesTheActionAndItsFindingAndTheActionStopsBeingOverdue() {
        fixture.clock.moveTo(DUE_DATE.plusDays(5));
        assertTrue(fixture.action(finding, rechargeAction).isOverdueOn(LocalDate.now(fixture.clock)));

        fixture.verify(finding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED, "Recharged, gauge reads 7 bar");

        assertTrue(fixture.action(finding, rechargeAction).isClosed());
        assertFalse(fixture.finding(finding).isOpen());
        assertFalse(fixture.finding(finding).blocksCertification());
        assertFalse(fixture.action(finding, rechargeAction).isOverdueOn(LocalDate.now(fixture.clock)));
        assertEquals(FindingAudit.VERIFICATION_ACCEPTED, fixture.findingLog.historyOf(finding).getLast().action());
    }

    @Test
    void acceptingAnActionCancelsTheOtherOpenActionsOfTheFindingSoNoneIsLeftOverdue() {
        CorrectiveActionId replaceAction = fixture.plan(finding, "Replace the unit", fixture.maintenanceTechnician, DUE_DATE, fixture.assetResponsible);
        fixture.clock.moveTo(DUE_DATE.plusDays(5));

        fixture.verify(finding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED, "Recharged, gauge reads 7 bar");

        assertFalse(fixture.finding(finding).isOpen());
        assertTrue(fixture.action(finding, replaceAction).isCancelled());
        assertFalse(fixture.action(finding, replaceAction).isClosed());
        assertEquals(fixture.verifier, fixture.action(finding, replaceAction).cancellation().orElseThrow().cancelledBy());
        assertEquals(fixture.clock.instant(), fixture.action(finding, replaceAction).cancellation().orElseThrow().cancelledAt());
        assertFalse(fixture.action(finding, replaceAction).isOverdueOn(LocalDate.now(fixture.clock)));
        assertEquals(0, fixture.generateFindingsSummary.execute(fixture.extinguisher.code()).findings().getFirst().overdueActions());
        assertFalse(fixture.action(finding, rechargeAction).isCancelled());
    }

    @Test
    void rejectedVerificationCancelsNothing() {
        CorrectiveActionId replaceAction = fixture.plan(finding, "Replace the unit", fixture.maintenanceTechnician, DUE_DATE, fixture.assetResponsible);

        fixture.verify(finding, rechargeAction, fixture.verifier, VerificationResult.REJECTED, "Gauge still reads 5 bar");

        assertFalse(fixture.action(finding, replaceAction).isCancelled());
        assertFalse(fixture.action(finding, rechargeAction).isCancelled());
    }

    @Test
    void rejectedVerificationMustExplainWhyAndIsNotRecordedOtherwise() {
        assertThrows(VerificationNotesRequiredException.class, () -> fixture.verify(finding, rechargeAction, fixture.verifier, VerificationResult.REJECTED, " "));

        assertTrue(fixture.action(finding, rechargeAction).verifications().isEmpty());
    }

    @Test
    void actionCanBeDueTodayButNotBeforeItIsPlanned() {
        CorrectiveActionId dueToday = fixture.plan(finding, "Tag the unit as out of service", fixture.maintenanceTechnician, CertiflowFixture.TODAY, fixture.assetResponsible);

        assertEquals(CertiflowFixture.TODAY, fixture.action(finding, dueToday).dueDate());
        assertThrows(DueDateInThePastException.class, () -> fixture.plan(finding, "Replace the unit", fixture.maintenanceTechnician, CertiflowFixture.TODAY.minusDays(1), fixture.assetResponsible));
        assertEquals(2, fixture.finding(finding).actions().size());
    }

    @Test
    void whoeverIsResponsibleForTheActionCannotVerifyIt() {
        assertThrows(VerifierMustDifferFromResponsibleException.class, () -> fixture.verify(finding, rechargeAction, fixture.maintenanceTechnician, VerificationResult.ACCEPTED, "Looks fine to me"));

        assertTrue(fixture.action(finding, rechargeAction).verifications().isEmpty());
        assertTrue(fixture.finding(finding).isOpen());
    }

    @Test
    void namesakeOfTheResponsibleCanVerifyButTheResponsibleUnderAnotherSpellingCannot() {
        Person namesake = new Person(new PersonId("EMP-900"), fixture.maintenanceTechnician.name());
        Person sameTechnician = new Person(fixture.maintenanceTechnician.id(), "M. Maintenance");

        assertThrows(VerifierMustDifferFromResponsibleException.class, () -> fixture.verify(finding, rechargeAction, sameTechnician, VerificationResult.ACCEPTED, "Looks fine to me"));
        fixture.verify(finding, rechargeAction, namesake, VerificationResult.ACCEPTED, "Recharged");

        assertTrue(fixture.action(finding, rechargeAction).isClosed());
    }

    @Test
    void closedFindingAcceptsNoFurtherActionsNorVerifications() {
        fixture.verify(finding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED, "Done");

        assertThrows(FindingAlreadyClosedException.class, () -> fixture.plan(finding, "Replace the unit", fixture.maintenanceTechnician, DUE_DATE, fixture.assetResponsible));
        assertThrows(FindingAlreadyClosedException.class, () -> fixture.verify(finding, rechargeAction, fixture.verifier, VerificationResult.REJECTED, "Second opinion"));
    }

    @Test
    void actionCanOnlyBeVerifiedThroughTheFindingItBelongsTo() {
        FindingId anotherFinding = fixture.majorFindingOf(fixture.assignedInspection());

        assertThrows(CorrectiveActionNotInFindingException.class, () -> fixture.verify(anotherFinding, rechargeAction, fixture.verifier, VerificationResult.ACCEPTED, "Wrong finding"));
    }

    @Test
    void planningIsAuditedAndAttributedToWhoeverPlanned() {
        assertEquals(FindingAudit.ACTION_PLANNED, fixture.findingLog.historyOf(finding).getLast().action());
        assertEquals(fixture.assetResponsible, fixture.findingLog.historyOf(finding).getLast().performedBy());
    }

    @Test
    void actionRemembersWhoPlannedItAndWhen() {
        assertEquals(fixture.assetResponsible, fixture.action(finding, rechargeAction).plannedBy());
        assertEquals(fixture.clock.instant(), fixture.action(finding, rechargeAction).plannedAt());
    }
}
