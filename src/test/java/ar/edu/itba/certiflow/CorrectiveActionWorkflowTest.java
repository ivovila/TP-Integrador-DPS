package ar.edu.itba.certiflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.domain.Certificate;
import ar.edu.itba.certiflow.domain.CorrectiveAction;
import ar.edu.itba.certiflow.domain.DomainException;
import ar.edu.itba.certiflow.domain.Evidence;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

@org.junit.jupiter.api.Tag("integration")
class CorrectiveActionWorkflowTest {
    @Test
    void correctionRequiresEvidenceIndependentVerificationAndClosure() {
        var f = new Fixture();
        var i = f.closed("60");
        var a = f.plan(i);
        assertThrows(DomainException.class, () -> f.actions.close(a.id(), "inspector"));
        assertThrows(
                DomainException.class, () -> f.actions.submit(a.id(), List.of(), "technician"));
        f.actions.submit(a.id(), List.of(f.evidence(Evidence.Kind.PHOTO)), "technician");
        assertThrows(
                DomainException.class, () -> f.actions.verify(a.id(), true, "OK", "technician"));
        assertThrows(DomainException.class, () -> f.certificates.issue(i.id(), "issuer"));
        f.actions.verify(a.id(), true, "Verified independently", "inspector");
        assertThrows(DomainException.class, () -> f.certificates.issue(i.id(), "issuer"));
        assertEquals(CorrectiveAction.State.CLOSED, f.actions.close(a.id(), "inspector").state());
        assertEquals(
                Certificate.Status.ACTIVE,
                f.certificates.status(f.certificates.issue(i.id(), "issuer").id()));
    }

    @Test
    void rejectedCorrectionCanBeResubmittedWithoutLosingVerificationHistory() {
        var f = new Fixture();
        var a = f.plan(f.closed("60"));
        f.actions.submit(a.id(), List.of(f.evidence(Evidence.Kind.PHOTO)), "technician");
        var rejected = f.actions.verify(a.id(), false, "Insufficient proof", "inspector");
        assertEquals(CorrectiveAction.State.PLANNED, rejected.state());
        f.actions.submit(a.id(), List.of(f.evidence(Evidence.Kind.DOCUMENT)), "technician");
        f.actions.verify(a.id(), true, "Retest accepted", "inspector");
        var closed = f.actions.close(a.id(), "inspector");
        assertTrue(
                closed.history().stream()
                        .anyMatch(e -> e.operation().equals("VERIFICATION_REJECTED")));
        assertThrows(
                DomainException.class,
                () ->
                        f.actions.submit(
                                a.id(), List.of(f.evidence(Evidence.Kind.PHOTO)), "technician"));
    }

    @Test
    void dueDateIsInclusiveAndClosedActionsAreNotOverdue() {
        var f = new Fixture();
        var a = f.resolve(f.closed("60"));
        assertFalse(a.isOverdue(a.dueDate().plusDays(1)));
        var pending = f.plan(f.closed("60"));
        assertFalse(pending.isOverdue(pending.dueDate()));
        assertTrue(pending.isOverdue(pending.dueDate().plusDays(1)));
    }

    @Test
    void cannotPlanInThePastOrDuplicateAnAction() {
        var f = new Fixture();
        var i = f.closed("60");
        assertThrows(
                DomainException.class,
                () ->
                        f.actions.plan(
                                i.id(),
                                i.findings().get(0).id(),
                                "Fix",
                                "technician",
                                LocalDate.now(f.clock).minusDays(1),
                                "owner"));
        f.plan(i);
        assertThrows(DomainException.class, () -> f.plan(i));
    }

    @Test
    void supersededFindingsCannotBeResolvedOrUsedToClearNewRevision() {
        var f = new Fixture();
        var i = f.closed("60");
        var a = f.resolve(i);
        f.inspections.rectify(i.id(), "Recheck", "supervisor");
        assertThrows(DomainException.class, () -> f.actions.close(a.id(), "inspector"));
        var corrected = f.inspections.close(i.id(), "inspector");
        assertNotEquals(i.findings().get(0).id(), corrected.findings().get(0).id());
        assertThrows(DomainException.class, () -> f.certificates.issue(i.id(), "issuer"));
        assertTrue(f.reports.findings(i.id()).actions().isEmpty());
    }
}
