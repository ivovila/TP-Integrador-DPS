package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * Único punto de entrada a los comandos de un hallazgo y responsable de levantarlos:
 * toda no conformidad, al cerrar o al rectificar, termina en un hallazgo.
 */
public final class FindingService {

    private final FindingFactory findingFactory;
    private final AuditLog<FindingId> findingLog;

    public FindingService(FindingFactory findingFactory, AuditLog<FindingId> findingLog) {
        this.findingFactory = findingFactory;
        this.findingLog = findingLog;
    }

    public List<Finding> raiseFrom(InspectionClosed closure, Supplier<FindingId> nextId) {
        return raise(closure, closure.nonConformities(), nextId);
    }

    /**
     * Una rectificación levanta hallazgo por cada no conformidad que introduce, salvo que ya haya uno abierto
     * por esa misma no conformidad. Los hallazgos anteriores no se tocan (S6).
     */
    public List<Finding> raiseFrom(InspectionRectified rectification, List<Finding> assetFindings, Supplier<FindingId> nextId) {
        List<CriterionResponse<?>> unaddressed = rectification.nonConformities().stream()
                .filter(nonConformity -> assetFindings.stream()
                        .noneMatch(finding -> finding.isOpenFor(rectification.inspectionId(), nonConformity)))
                .toList();
        return raise(rectification, unaddressed, nextId);
    }

    public CorrectiveAction planAction(Finding finding, ActionPlan plan, Stamp planned, LocalDate plannedOn) {
        CorrectiveAction action = finding.planAction(plan, planned, plannedOn);
        recorded(finding);
        return action;
    }

    public Verification verifyAction(Finding finding, CorrectiveActionId actionId, VerificationResult verificationResult,
                                     String verificationNotes, Stamp verified) {
        Verification verification = finding.verifyAction(actionId, verificationResult, verificationNotes, verified);
        recorded(finding);
        return verification;
    }

    private List<Finding> raise(RaisesFindings source, List<CriterionResponse<?>> nonConformities, Supplier<FindingId> nextId) {
        List<Finding> raisedFindings = nonConformities.stream()
                .map(nonConformity -> findingFactory.create(nextId.get(), source, nonConformity))
                .toList();
        raisedFindings.forEach(this::recorded);
        return raisedFindings;
    }

    private void recorded(Finding finding) {
        findingLog.recordAll(finding.id(), finding.pullAuditEntries());
    }
}
