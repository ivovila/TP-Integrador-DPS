package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.audit.AuditAction;

public enum CertificateAudit implements AuditAction {
    ISSUED("Certificate issued"),
    SUSPENDED("Certificate suspended"),
    RENEWED("Certificate renewed");

    private final String label;

    CertificateAudit(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
