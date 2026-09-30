package ar.edu.itba.certiflow.models.certificate;

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
            return CertificateStatus.SUSPENDED;
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
