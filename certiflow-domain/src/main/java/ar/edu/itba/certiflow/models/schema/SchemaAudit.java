package ar.edu.itba.certiflow.models.schema;

import ar.edu.itba.certiflow.models.audit.AuditAction;

public enum SchemaAudit implements AuditAction {
    CREATED("Inspection schema created"),
    VERSION_ADDED("Schema version added");

    private final String label;

    SchemaAudit(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
