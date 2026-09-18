package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.audit.AuditAction;

public enum FindingAudit implements AuditAction {
    RAISED("Finding raised"),
    ACTION_PLANNED("Corrective action planned"),
    VERIFICATION_ACCEPTED("Verification accepted: action and finding closed"),
    VERIFICATION_REJECTED("Verification rejected: action remains open");

    private final String label;

    FindingAudit(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
