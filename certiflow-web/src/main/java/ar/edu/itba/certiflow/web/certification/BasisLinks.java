package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.model.CertificateBasisDescriber;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.InspectionReference;
import ar.edu.itba.certiflow.certification.model.PolicyCode;
import ar.edu.itba.certiflow.web.support.Uris;
import java.time.Instant;
import java.util.List;
import org.springframework.hateoas.Link;

/** Los links hacia aquello en lo que se apoya un certificado: su inspección o sus parciales. */
final class BasisLinks implements CertificateBasisDescriber<List<Link>> {

    @Override
    public List<Link> inspection(InspectionReference inspection) {
        return List.of(Link.of(Uris.path("inspections", inspection.id().value())).withRel("inspection"));
    }

    @Override
    public List<Link> partialCertificates(List<CertificateNumber> partialCertificateNumbers, PolicyCode globalPolicyCode,
                                          Instant latestInspectionClosedAt) {
        return partialCertificateNumbers.stream()
                .map(partialCertificateNumber -> Link.of(Uris.path("certificates", partialCertificateNumber.value()))
                        .withRel("partial-certificates"))
                .toList();
    }
}
