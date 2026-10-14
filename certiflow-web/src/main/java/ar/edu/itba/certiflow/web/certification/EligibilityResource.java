package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.DecisionPayload;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.ReasonPayload;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

public class EligibilityResource extends RepresentationModel<EligibilityResource> {

    public final boolean eligible;
    public final DecisionPayload decision;
    public final List<ReasonPayload> ineligibilities;

    EligibilityResource(CertificationDecision decision) {
        this.eligible = decision.isEligible();
        this.decision = DecisionPayload.of(decision);
        this.ineligibilities = decision.describedBy(new IneligibilityReasons());
    }
}
