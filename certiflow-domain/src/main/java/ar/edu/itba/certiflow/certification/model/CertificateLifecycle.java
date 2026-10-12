package ar.edu.itba.certiflow.certification.model;

import java.time.LocalDate;

enum CertificateLifecycle {
    ISSUED {
        @Override
        CertificateStatus statusOn(ValidityPeriod validity, LocalDate referenceDate) {
            return validity.covers(referenceDate) ? CertificateStatus.ACTIVE : CertificateStatus.EXPIRED;
        }
    },
    SUSPENDED {
        @Override
        CertificateStatus statusOn(ValidityPeriod validity, LocalDate referenceDate) {
            return validity.covers(referenceDate) ? CertificateStatus.SUSPENDED : CertificateStatus.EXPIRED;
        }
    },
    RENEWED {
        @Override
        CertificateStatus statusOn(ValidityPeriod validity, LocalDate referenceDate) {
            return CertificateStatus.RENEWED;
        }
    };

    abstract CertificateStatus statusOn(ValidityPeriod validity, LocalDate referenceDate);
}
