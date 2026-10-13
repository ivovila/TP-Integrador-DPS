package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.AuditAction;

public enum InspectionAudit implements AuditAction {
    ASSIGNED("Inspection assigned"),
    ANSWER_RECORDED("Answer recorded"),
    EVIDENCE_ATTACHED("Evidence attached"),
    OBSERVATION_ADDED("Observation added"),
    CLOSED("Inspection closed"),
    RECTIFIED("Inspection rectified");

    private final String label;

    InspectionAudit(String label) {
        this.label = label;
    }

    @Override
    public String code() {
        return "InspectionAudit." + name();
    }

    @Override
    public String label() {
        return label;
    }
}
