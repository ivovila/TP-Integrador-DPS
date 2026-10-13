package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
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
        return recorded(inspection);
    }

    public <A extends Answer> void recordAnswer(Inspection inspection, Criterion<A> criterion, A answer, Stamp answered) {
        inspection.recordAnswer(criterion, answer, answered);
        recorded(inspection);
    }

    public void attachEvidence(Inspection inspection, Criterion<?> criterion, Evidence evidence, Stamp attached) {
        inspection.attachEvidence(criterion, evidence, attached);
        recorded(inspection);
    }

    public void addObservation(Inspection inspection, Criterion<?> criterion, String observationText, Stamp observed) {
        inspection.addObservation(criterion, observationText, observed);
        recorded(inspection);
    }

    public InspectionClosed close(Inspection inspection, Stamp closed) {
        InspectionClosed closure = inspection.close(closed);
        recorded(inspection);
        return closure;
    }

    public <A extends Answer> InspectionRectified rectify(Inspection inspection, Criterion<A> criterion, A answer,
                                                          String reason, Stamp rectified) {
        InspectionRectified rectification = inspection.rectify(criterion, answer, reason, rectified);
        recorded(inspection);
        return rectification;
    }

    public void recordAnswer(Inspection inspection, String criterionCode, RawAnswer answer, Stamp answered) {
        recordTyped(inspection, inspection.schemaVersion().criterion(criterionCode), answer, answered);
    }

    public void attachEvidence(Inspection inspection, String criterionCode, Evidence evidence, Stamp attached) {
        attachEvidence(inspection, inspection.schemaVersion().criterion(criterionCode), evidence, attached);
    }

    public void addObservation(Inspection inspection, String criterionCode, String observationText, Stamp observed) {
        addObservation(inspection, inspection.schemaVersion().criterion(criterionCode), observationText, observed);
    }

    public InspectionRectified rectify(Inspection inspection, String criterionCode, RawAnswer answer, String reason,
                                       Stamp rectified) {
        return rectifyTyped(inspection, inspection.schemaVersion().criterion(criterionCode), answer, reason, rectified);
    }

    private <A extends Answer> void recordTyped(Inspection inspection, Criterion<A> criterion, RawAnswer answer, Stamp answered) {
        recordAnswer(inspection, criterion, criterion.answerFrom(answer), answered);
    }

    private <A extends Answer> InspectionRectified rectifyTyped(Inspection inspection, Criterion<A> criterion, RawAnswer answer,
                                                                String reason, Stamp rectified) {
        return rectify(inspection, criterion, criterion.answerFrom(answer), reason, rectified);
    }

    private Inspection recorded(Inspection inspection) {
        inspectionLog.recordAll(inspection.id(), inspection.pullAuditEntries());
        return inspection;
    }
}
