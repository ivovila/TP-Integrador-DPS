package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.AuditAction;

public enum FindingAudit implements AuditAction {
    RAISED("Finding raised"),
    ACTION_PLANNED("Corrective action planned"),
    VERIFICATION_ACCEPTED("Verification accepted: action and finding closed, remaining actions cancelled"),
    VERIFICATION_REJECTED("Verification rejected: action remains open");

    private final String label;

    FindingAudit(String label) {
        this.label = label;
    }

    @Override
    public String code() {
        return "FindingAudit." + name();
    }

    @Override
    public String label() {
        return label;
    }
}
