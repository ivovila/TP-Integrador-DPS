package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

public class EligibilityResource extends RepresentationModel<EligibilityResource> {

    public record Reason(String reason, String detail) {
    }

    public final boolean eligible;
    public final List<Reason> ineligibilities;

    EligibilityResource(CertificationDecision decision) {
        this.eligible = decision.isEligible();
        this.ineligibilities = decision.ineligibilities().stream()
                .map(ineligibility -> new Reason(ineligibility.reason().name(), ineligibility.detail()))
                .toList();
    }
}
