package ar.edu.itba.certiflow.certification.model;

public enum CertificateStatus {
    DEPENDENCY_INVALID(false),
    ACTIVE(true) {
        @Override
        CertificateStatus withoutActiveBacking() {
            return DEPENDENCY_INVALID;
        }
    },
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

    /** Solo un certificado activo deja de serlo cuando sus respaldos ya no lo están. */
    CertificateStatus withoutActiveBacking() {
        return this;
    }
}
