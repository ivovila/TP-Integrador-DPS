package ar.edu.itba.certiflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.domain.Answer;
import ar.edu.itba.certiflow.domain.DomainException;
import ar.edu.itba.certiflow.domain.Evidence;
import ar.edu.itba.certiflow.domain.Outcome;
import ar.edu.itba.certiflow.domain.Severity;
import ar.edu.itba.certiflow.domain.Submission;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@org.junit.jupiter.api.Tag("integration")
class InspectionWorkflowTest {
    @Test
    void versionIsSelectedAtStartAndRemainsFrozenAfterNewPublication() {
        var f = new Fixture();
        var assigned = f.assigned();
        f.publish("45");
        var started = f.inspections.start(assigned.id(), "inspector");
        f.publish("40");
        f.inspections.record(started.id(), "TEMP", f.numeric("43"), "inspector");
        var evaluated = f.inspections.evaluate(started.id(), "inspector");
        assertEquals(2, evaluated.version().orElseThrow().number());
        assertEquals(Outcome.OBSERVED, evaluated.evaluations().get("TEMP").outcome());
        assertEquals(3, f.started().version().orElseThrow().number());
    }

    @Test
    void incompleteClosureDoesNotPersistPartialChangesOrAuditEntries() {
        var f = new Fixture();
        var i = f.started();
        assertThrows(DomainException.class, () -> f.inspections.close(i.id(), "inspector"));
        assertSame(i, f.inspections.get(i.id()));
        assertTrue(i.evaluations().isEmpty());
    }

    @Test
    void closedInspectionRejectsChangesAndDuplicateClosureWithoutDuplicatingFindings() {
        var f = new Fixture();
        var i = f.closed("60");
        assertEquals(1, i.findings().size());
        assertEquals(Severity.CRITICAL, i.findings().get(0).severity());
        assertEquals("owner", i.findings().get(0).responsible());
        assertFalse(i.findings().get(0).evidence().isEmpty());
        assertThrows(
                DomainException.class,
                () -> f.inspections.record(i.id(), "TEMP", f.numeric("20"), "inspector"));
        assertThrows(DomainException.class, () -> f.inspections.close(i.id(), "inspector"));
        assertSame(i, f.inspections.get(i.id()));
    }

    @Test
    void rectificationPreservesOriginalAnswersResultsFindingsAndVersion() {
        var f = new Fixture();
        var original = f.closed("60");
        f.publish("40");
        var reopened = f.inspections.rectify(original.id(), "Error de transcripcion", "supervisor");
        assertEquals(2, reopened.revision());
        assertTrue(reopened.evaluations().isEmpty());
        f.inspections.record(original.id(), "TEMP", f.numeric("20"), "inspector");
        var corrected = f.inspections.close(original.id(), "inspector");
        assertTrue(corrected.findings().isEmpty());
        var preserved = corrected.previousRevisions().get(0);
        assertEquals(original.submissions(), preserved.submissions());
        assertEquals(original.evaluations(), preserved.evaluations());
        assertEquals(original.findings(), preserved.findings());
        assertEquals(1, corrected.version().orElseThrow().number());
        assertTrue(
                corrected.history().stream()
                        .anyMatch(
                                e ->
                                        e.operation().equals("RECTIFICATION_OPENED")
                                                && e.detail().contains("Error de transcripcion")));
    }

    @Test
    void updatingAnAnswerInvalidatesPreviousEvaluationAndKeepsBothValuesInAudit() {
        var f = new Fixture();
        var i = f.started();
        f.inspections.record(i.id(), "TEMP", f.numeric("20"), "inspector");
        f.inspections.evaluate(i.id(), "inspector");
        var updated = f.inspections.record(i.id(), "TEMP", f.numeric("60"), "inspector");
        assertTrue(updated.evaluations().isEmpty());
        var entry = updated.history().get(updated.history().size() - 1);
        assertTrue(entry.detail().contains("value=20"));
        assertTrue(entry.detail().contains("value=60"));
    }

    @Test
    void modelAndRepositorySnapshotsCannotBeMutatedFromOutside() {
        var f = new Fixture();
        var i = f.closed("20");
        assertThrows(UnsupportedOperationException.class, () -> i.submissions().clear());
        assertThrows(UnsupportedOperationException.class, () -> i.history().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> i.version().orElseThrow().sections().clear());
        var list = new ArrayList<Evidence>();
        list.add(f.evidence(Evidence.Kind.PHOTO));
        var s = new Submission(new Answer.YesNo(true), list, "");
        list.clear();
        assertEquals(1, s.evidence().size());
        assertThrows(UnsupportedOperationException.class, () -> f.catalog.assets().clear());
    }

    @Test
    void assignmentRequiresCompatibleAssetTypeAndKnownScheme() {
        var f = new Fixture();
        var other = f.catalog.registerAsset("Motor", "ENGINE", "Plant", "owner", Map.of());
        assertThrows(
                DomainException.class,
                () ->
                        f.inspections.assign(
                                other.id(),
                                f.schemeId,
                                "inspector",
                                java.time.LocalDate.now(f.clock),
                                "All",
                                "coordinator"));
        assertThrows(
                DomainException.class,
                () ->
                        f.inspections.assign(
                                f.asset.id(),
                                UUID.randomUUID(),
                                "inspector",
                                java.time.LocalDate.now(f.clock),
                                "All",
                                "coordinator"));
    }

    @Test
    void assignedInspectionCannotReceiveAnswersAndUnknownCriteriaAreRejected() {
        var f = new Fixture();
        var assigned = f.assigned();
        assertThrows(
                DomainException.class,
                () -> f.inspections.record(assigned.id(), "TEMP", f.numeric("20"), "inspector"));
        var started = f.inspections.start(assigned.id(), "inspector");
        assertThrows(
                DomainException.class,
                () -> f.inspections.record(started.id(), "UNKNOWN", f.numeric("20"), "inspector"));
        assertThrows(DomainException.class, () -> f.inspections.start(started.id(), "inspector"));
        assertThrows(
                DomainException.class,
                () -> f.inspections.rectify(started.id(), "Reason", "supervisor"));
    }

    @Test
    void publishedVersionsCannotBeOverwrittenOrChangeAssetType() {
        var f = new Fixture();
        var v = f.schemeRepo.versions(f.schemeId).get(0);
        assertThrows(DomainException.class, () -> f.schemeRepo.publish(v));
        assertThrows(
                DomainException.class,
                () ->
                        f.catalog.publish(
                                f.schemeId, "Other", "ENGINE", v.sections(), 365, "author"));
    }
}
