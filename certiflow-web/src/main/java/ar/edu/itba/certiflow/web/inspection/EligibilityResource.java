package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.DecisionPayload;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

public class EligibilityResource extends RepresentationModel<EligibilityResource> {

    public record Reason(String reason, String detail) {
    }

    public final boolean eligible;
    public final DecisionPayload decision;
    public final List<Reason> ineligibilities;

    EligibilityResource(CertificationDecision decision) {
        this.eligible = decision.isEligible();
        this.decision = DecisionPayload.of(decision);
        this.ineligibilities = decision.ineligibilities().stream()
                .map(ineligibility -> new Reason(ineligibility.reason().name(), ineligibility.detail()))
                .toList();
    }
}
