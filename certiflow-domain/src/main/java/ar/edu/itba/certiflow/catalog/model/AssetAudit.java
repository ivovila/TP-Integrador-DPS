package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.AuditAction;

public enum AssetAudit implements AuditAction {
    REGISTERED("Asset registered");

    private final String label;

    AssetAudit(String label) {
        this.label = label;
    }

    @Override
    public String code() {
        return "AssetAudit." + name();
    }

    @Override
    public String label() {
        return label;
    }
}
