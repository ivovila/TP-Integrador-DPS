package ar.edu.itba.certiflow.domain.model.certificate;

import java.util.EnumSet;
import java.util.Set;

public enum CertificateStatus {
    ISSUED {
        @Override
        public Set<CertificateStatus> next() {
            return EnumSet.of(SUSPENDED, EXPIRED, RENEWED);
        }

        @Override
        public boolean isValid() {
            return true;
        }
    },
    SUSPENDED {
        @Override
        public Set<CertificateStatus> next() {
            return EnumSet.of(ISSUED, EXPIRED);
        }
    },
    EXPIRED {
        @Override
        public Set<CertificateStatus> next() {
            return EnumSet.noneOf(CertificateStatus.class);
        }
    },
    RENEWED {
        @Override
        public Set<CertificateStatus> next() {
            return EnumSet.noneOf(CertificateStatus.class);
        }
    };

    public abstract Set<CertificateStatus> next();

    public boolean isValid() {
        return false;
    }
}
