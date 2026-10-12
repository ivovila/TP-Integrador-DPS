package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;

/**
 * Único punto de entrada a los comandos de una inspección: los métodos del agregado son de paquete,
 * así que ningún caso de uso puede cambiar una inspección sin pasar por acá ni olvidarse de la bitácora.
 */
public final class InspectionService {

    private final InspectionFactory inspectionFactory;
    private final AuditLog<Inspection> inspectionLog;

    public InspectionService(InspectionFactory inspectionFactory, AuditLog<Inspection> inspectionLog) {
        this.inspectionFactory = inspectionFactory;
        this.inspectionLog = inspectionLog;
    }

    public Inspection assign(InspectionId inspectionId, Asset asset, InspectionAssignment assignment,
                             SchemaVersion schemaVersion, Person assignedBy, Instant assignedAt) {
        Inspection inspection = inspectionFactory.create(inspectionId, asset, assignment, schemaVersion, assignedBy, assignedAt);
        return recorded(inspection);
    }

    public <A extends Answer> void recordAnswer(Inspection inspection, Criterion<A> criterion, A answer,
                                                Person answeredBy, Instant answeredAt) {
        inspection.recordAnswer(criterion, answer, answeredBy, answeredAt);
        recorded(inspection);
    }

    public void attachEvidence(Inspection inspection, Criterion<?> criterion, Evidence evidence, Person attachedBy, Instant attachedAt) {
        inspection.attachEvidence(criterion, evidence, attachedBy, attachedAt);
        recorded(inspection);
    }

    public void addObservation(Inspection inspection, Criterion<?> criterion, String observationText, Person observedBy, Instant observedAt) {
        inspection.addObservation(criterion, observationText, observedBy, observedAt);
        recorded(inspection);
    }

    public InspectionClosed close(Inspection inspection, Person closedBy, Instant closedAt) {
        InspectionClosed closure = inspection.close(closedBy, closedAt);
        recorded(inspection);
        return closure;
    }

    public <A extends Answer> InspectionRectified rectify(Inspection inspection, Criterion<A> criterion, A answer,
                                                          String reason, Person rectifiedBy, Instant rectifiedAt) {
        InspectionRectified rectification = inspection.rectify(criterion, answer, reason, rectifiedBy, rectifiedAt);
        recorded(inspection);
        return rectification;
    }

    private Inspection recorded(Inspection inspection) {
        inspectionLog.recordAll(inspection, inspection.pullAuditEntries());
        return inspection;
    }
}
