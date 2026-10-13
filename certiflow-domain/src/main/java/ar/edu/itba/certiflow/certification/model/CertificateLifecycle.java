package ar.edu.itba.certiflow.certification.model;

import java.time.LocalDate;

public enum CertificateLifecycle {
    ISSUED(false) {
        @Override
        CertificateStatus statusOn(ValidityPeriod validity, LocalDate referenceDate) {
            return validity.covers(referenceDate) ? CertificateStatus.ACTIVE : CertificateStatus.EXPIRED;
        }
    },
    SUSPENDED(false) {
        @Override
        CertificateStatus statusOn(ValidityPeriod validity, LocalDate referenceDate) {
            return validity.covers(referenceDate) ? CertificateStatus.SUSPENDED : CertificateStatus.EXPIRED;
        }
    },
    RENEWED(true) {
        @Override
        CertificateStatus statusOn(ValidityPeriod validity, LocalDate referenceDate) {
            return CertificateStatus.RENEWED;
        }
    };

    private final boolean replaced;

    CertificateLifecycle(boolean replaced) {
        this.replaced = replaced;
    }

    abstract CertificateStatus statusOn(ValidityPeriod validity, LocalDate referenceDate);

    boolean isReplaced() {
        return replaced;
    }
}
