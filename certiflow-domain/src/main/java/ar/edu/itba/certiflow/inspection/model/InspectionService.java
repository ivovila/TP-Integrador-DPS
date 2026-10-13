package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Stamp;

/**
 * Único punto de entrada a los comandos de una inspección: los métodos del agregado son de paquete,
 * así que ningún caso de uso puede cambiar una inspección sin pasar por acá ni olvidarse de la bitácora.
 */
public final class InspectionService {

    private final InspectionFactory inspectionFactory;
    private final AuditLog<InspectionId> inspectionLog;

    public InspectionService(InspectionFactory inspectionFactory, AuditLog<InspectionId> inspectionLog) {
        this.inspectionFactory = inspectionFactory;
        this.inspectionLog = inspectionLog;
    }

    public Inspection assign(InspectionId inspectionId, Asset asset, InspectionAssignment assignment,
                             SchemaVersion schemaVersion, Stamp assigned) {
        Inspection inspection = inspectionFactory.create(inspectionId, asset, assignment, schemaVersion, assigned);
        return recordAuditEntriesOf(inspection);
    }

    public void recordAnswer(Inspection inspection, CriterionCode criterionCode, RawAnswer answer, Stamp answered) {
        recordTyped(inspection, inspection.schemaVersion().criterion(criterionCode), answer, answered);
    }

    public void attachEvidence(Inspection inspection, CriterionCode criterionCode, Evidence evidence, Stamp attached) {
        inspection.attachEvidence(inspection.schemaVersion().criterion(criterionCode), evidence, attached);
        recordAuditEntriesOf(inspection);
    }

    public void addObservation(Inspection inspection, CriterionCode criterionCode, String observationText, Stamp observed) {
        inspection.addObservation(inspection.schemaVersion().criterion(criterionCode), observationText, observed);
        recordAuditEntriesOf(inspection);
    }

    public InspectionClosed close(Inspection inspection, Stamp closed) {
        InspectionClosed closure = inspection.close(closed);
        recordAuditEntriesOf(inspection);
        return closure;
    }

    public InspectionRectified rectify(Inspection inspection, CriterionCode criterionCode, RawAnswer answer, String reason,
                                       Stamp rectified) {
        return rectifyTyped(inspection, inspection.schemaVersion().criterion(criterionCode), answer, reason, rectified);
    }

    private <A extends Answer> void recordTyped(Inspection inspection, Criterion<A> criterion, RawAnswer answer, Stamp answered) {
        inspection.recordAnswer(criterion, criterion.answerFrom(answer), answered);
        recordAuditEntriesOf(inspection);
    }

    private <A extends Answer> InspectionRectified rectifyTyped(Inspection inspection, Criterion<A> criterion, RawAnswer answer,
                                                                String reason, Stamp rectified) {
        InspectionRectified rectification = inspection.rectify(criterion, criterion.answerFrom(answer), reason, rectified);
        recordAuditEntriesOf(inspection);
        return rectification;
    }

    private Inspection recordAuditEntriesOf(Inspection inspection) {
        inspectionLog.recordAll(inspection.id(), inspection.pullAuditEntries());
        return inspection;
    }
}
