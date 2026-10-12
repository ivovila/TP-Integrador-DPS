package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
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
        return raise(closure.nonConformities(), closure.inspectionId(), closure.assetCode(), closure.assetResponsible(),
                closure.closedBy(), closure.closedAt(), nextId);
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
        return raise(unaddressed, rectification.inspectionId(), rectification.assetCode(), rectification.assetResponsible(),
                rectification.rectifiedBy(), rectification.rectifiedAt(), nextId);
    }

    public CorrectiveAction planAction(Finding finding, String description, Person actionResponsible, LocalDate dueDate,
                                       Person plannedBy, Instant plannedAt, LocalDate plannedOn) {
        CorrectiveAction action = finding.planAction(description, actionResponsible, dueDate, plannedBy, plannedAt, plannedOn);
        recorded(finding);
        return action;
    }

    public Verification verifyAction(Finding finding, CorrectiveActionId actionId, Person verifier,
                                     VerificationResult verificationResult, String verificationNotes, Instant verifiedAt) {
        Verification verification = finding.verifyAction(actionId, verifier, verificationResult, verificationNotes, verifiedAt);
        recorded(finding);
        return verification;
    }

    private List<Finding> raise(List<CriterionResponse<?>> nonConformities, InspectionId inspectionId, AssetCode assetCode,
                                Person responsible, Person raisedBy, Instant raisedAt, Supplier<FindingId> nextId) {
        List<Finding> raisedFindings = nonConformities.stream()
                .map(nonConformity -> findingFactory.create(nextId.get(), inspectionId, assetCode, responsible, nonConformity, raisedBy, raisedAt))
                .toList();
        raisedFindings.forEach(this::recorded);
        return raisedFindings;
    }

    private void recorded(Finding finding) {
        findingLog.recordAll(finding.id(), finding.pullAuditEntries());
    }
}
