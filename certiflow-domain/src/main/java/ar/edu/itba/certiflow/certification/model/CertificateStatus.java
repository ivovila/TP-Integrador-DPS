package ar.edu.itba.certiflow.certification.model;

public enum CertificateStatus {
    DEPENDENCY_INVALID(false),
    ACTIVE(true),
    EXPIRED(false),
    SUSPENDED(false),
    RENEWED(false);

    private final boolean active;

    CertificateStatus(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
