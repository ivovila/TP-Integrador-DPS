package ar.edu.itba.certiflow.domain.model.finding;

import static ar.edu.itba.certiflow.support.Fixtures.NOW;
import static ar.edu.itba.certiflow.support.Fixtures.PRESSURE;
import static ar.edu.itba.certiflow.support.Fixtures.SIGNAGE;
import static ar.edu.itba.certiflow.support.Fixtures.photo;
import static ar.edu.itba.certiflow.support.Fixtures.pressureResponse;
import static ar.edu.itba.certiflow.support.Fixtures.signageWithPhoto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.support.Fixtures;

class FindingTest {

    private final Inspection inspection = Fixtures.closedInspection(Fixtures.extinguisher(),
            Fixtures.extinguisherSchema(), pressureResponse("14").withEvidence(photo()), signageWithPhoto());
    private final PersonId responsible = PersonId.generate();
    private final LocalDate today = NOW.toLocalDate();

    private FindingDetails pressureDetails() {
        return new FindingDetails(PRESSURE, Severity.CRITICAL, "Presion fuera de rango", responsible);
    }

    private Finding raise() {
        return Finding.raise(FindingId.generate(), inspection.getEvaluation(), pressureDetails(), List.of(), NOW);
    }

    @Test
    void findingIsRaisedOnlyOnNonApprovedCriteria() {
        FindingDetails signage = new FindingDetails(SIGNAGE, Severity.MINOR, "Cartel", responsible);

        assertThrows(DomainException.class, () -> Finding.raise(FindingId.generate(), inspection.getEvaluation(),
                signage, List.of(), NOW));
    }

    @Test
    void criterionCannotHaveTwoFindingsInTheSameInspection() {
        List<Finding> existing = List.of(raise());

        assertThrows(DomainException.class, () -> Finding.raise(FindingId.generate(), inspection.getEvaluation(),
                pressureDetails(), existing, NOW));
    }

    @Test
    void findingsFromAnotherInspectionAreRejected() {
        Inspection other = Fixtures.closedInspection(Fixtures.extinguisher(), Fixtures.extinguisherSchema(),
                pressureResponse("14"), signageWithPhoto());
        List<Finding> foreign = List.of(Finding.raise(FindingId.generate(), other.getEvaluation(),
                pressureDetails(), List.of(), NOW));

        assertThrows(IllegalArgumentException.class, () -> Finding.raise(FindingId.generate(),
                inspection.getEvaluation(), pressureDetails(), foreign, NOW));
    }

    @Test
    void findingKeepsTheEvidenceRecordedForTheCriterion() {
        Finding finding = raise();

        assertEquals(1, finding.getEvidences().size());
        assertEquals(inspection.getAsset(), finding.getAsset());
        assertTrue(finding.isOpen());
    }

    @Test
    void findingClosesOnlyWhenAllActionsAreVerified() {
        Finding finding = raise();
        assertThrows(DomainException.class, () -> finding.close(NOW));

        CorrectiveAction action = finding.planAction(CorrectiveActionId.generate(), "Recargar matafuego",
                responsible, today.plusDays(15), NOW);
        assertThrows(DomainException.class, () -> finding.close(NOW));

        finding.verifyAction(action.getId(), PersonId.generate(), NOW.plusDays(3));
        finding.close(NOW.plusDays(3));

        assertFalse(finding.isOpen());
        assertThrows(InvalidTransitionException.class, () -> finding.planAction(CorrectiveActionId.generate(),
                "Otra", responsible, today.plusDays(1), NOW));
    }

    @Test
    void whoExecutesTheActionCannotVerifyIt() {
        Finding finding = raise();
        CorrectiveAction action = finding.planAction(CorrectiveActionId.generate(), "Recargar matafuego",
                responsible, today.plusDays(15), NOW);

        assertThrows(DomainException.class, () -> finding.verifyAction(action.getId(), responsible, NOW));
    }

    @Test
    void unverifiedActionIsOverdueAfterItsDueDate() {
        Finding finding = raise();
        CorrectiveAction action = finding.planAction(CorrectiveActionId.generate(), "Recargar matafuego",
                responsible, today.plusDays(10), NOW);

        assertFalse(finding.hasOverdueActions(today.plusDays(10)));
        assertTrue(finding.hasOverdueActions(today.plusDays(11)));

        finding.verifyAction(action.getId(), PersonId.generate(), NOW.plusDays(12));
        assertFalse(finding.hasOverdueActions(today.plusDays(12)));
    }

    @Test
    void dueDateCannotBeInThePast() {
        Finding finding = raise();

        assertThrows(DomainException.class, () -> finding.planAction(CorrectiveActionId.generate(),
                "Recargar", responsible, today.minusDays(1), NOW));
    }
}
