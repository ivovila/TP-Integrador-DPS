package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.audit.AuditAction;

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
    public String label() {
        return label;
    }
}
