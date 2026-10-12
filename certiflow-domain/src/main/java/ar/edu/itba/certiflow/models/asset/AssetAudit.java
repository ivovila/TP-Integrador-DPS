package ar.edu.itba.certiflow.models.asset;

import ar.edu.itba.certiflow.models.audit.AuditAction;

public enum AssetAudit implements AuditAction {
    REGISTERED("Asset registered");

    private final String label;

    AssetAudit(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
