package ar.edu.itba.certiflow.certification.model;

import java.time.Instant;
import java.util.List;

public sealed interface CertificateBasis permits CertificateBasis.InspectionBasis, CertificateBasis.PartialCertificatesBasis {
    record InspectionBasis(InspectionReference inspection) implements CertificateBasis { }
    record PartialCertificatesBasis(List<CertificateNumber> partials, String globalPolicy,
            Instant latestInspectionClosedAt) implements CertificateBasis {
        public PartialCertificatesBasis { partials = List.copyOf(partials); }
    }
}
