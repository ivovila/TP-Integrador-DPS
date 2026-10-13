package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.model.CertificateBasisDescriber;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.InspectionReference;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** La inspección en la que se apoya un certificado, si se apoya en una. */
final class BasisInspectionId implements CertificateBasisDescriber<Optional<String>> {

    @Override
    public Optional<String> inspection(InspectionReference inspection) {
        return Optional.of(inspection.id().value());
    }

    @Override
    public Optional<String> partialCertificates(List<CertificateNumber> partialCertificateNumbers, String globalPolicyCode,
                                                Instant latestInspectionClosedAt) {
        return Optional.empty();
    }
}
