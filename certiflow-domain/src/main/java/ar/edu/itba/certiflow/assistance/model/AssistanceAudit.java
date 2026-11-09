package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.shared.model.AuditAction;

public enum AssistanceAudit implements AuditAction {
    REVIEW_REQUESTED("Assistant review requested"),
    SUGGESTION_ACCEPTED("Assistant suggestion accepted"),
    SUGGESTION_DISMISSED("Assistant suggestion dismissed");

    private final String label;

    AssistanceAudit(String label) {
        this.label = label;
    }

    @Override
    public String code() {
        return "AssistanceAudit." + name();
    }

    @Override
    public String label() {
        return label;
    }
}
