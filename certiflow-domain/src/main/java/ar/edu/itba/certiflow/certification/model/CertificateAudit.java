package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.AuditAction;

public enum CertificateAudit implements AuditAction {
    ISSUED("Certificate issued"),
    SUSPENDED("Certificate suspended"),
    RENEWED("Certificate renewed");

    private final String label;

    CertificateAudit(String label) {
        this.label = label;
    }

    @Override
    public String code() {
        return "CertificateAudit." + name();
    }

    @Override
    public String label() {
        return label;
    }
}
