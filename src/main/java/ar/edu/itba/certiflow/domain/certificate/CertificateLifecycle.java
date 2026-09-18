package ar.edu.itba.certiflow.domain.certificate;

import java.time.LocalDate;

enum CertificateLifecycle {
    ISSUED {
        @Override
        CertificateStatus statusOn(ValidityPeriod validity, LocalDate date) {
            return validity.covers(date) ? CertificateStatus.ACTIVE : CertificateStatus.EXPIRED;
        }
    },
    SUSPENDED {
        @Override
        CertificateStatus statusOn(ValidityPeriod validity, LocalDate date) {
            return CertificateStatus.SUSPENDED;
        }
    },
    RENEWED {
        @Override
        CertificateStatus statusOn(ValidityPeriod validity, LocalDate date) {
            return CertificateStatus.RENEWED;
        }
    };

    abstract CertificateStatus statusOn(ValidityPeriod validity, LocalDate date);
}
