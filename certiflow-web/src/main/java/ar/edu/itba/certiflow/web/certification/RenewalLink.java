package ar.edu.itba.certiflow.web.certification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import ar.edu.itba.certiflow.certification.model.CertificateBasisDescriber;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.InspectionReference;
import ar.edu.itba.certiflow.certification.model.PolicyCode;
import java.time.Instant;
import java.util.List;
import org.springframework.hateoas.Link;

/** Cómo se renueva un certificado según en qué se apoya: con otra inspección o con otros parciales. */
final class RenewalLink implements CertificateBasisDescriber<Link> {

    private final String certificateNumber;

    RenewalLink(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    @Override
    public Link inspection(InspectionReference inspection) {
        return linkTo(CertificateController.class).slash(certificateNumber).slash("renewals").withRel("renewals");
    }

    @Override
    public Link partialCertificates(List<CertificateNumber> partialCertificateNumbers, PolicyCode globalPolicyCode,
                                    Instant latestInspectionClosedAt) {
        return linkTo(CertificateController.class).slash(certificateNumber).slash("global-renewals").withRel("global-renewals");
    }
}
