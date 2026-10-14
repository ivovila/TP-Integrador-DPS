package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationDecisionDescriber;
import ar.edu.itba.certiflow.certification.model.GrantedDecision;
import ar.edu.itba.certiflow.certification.model.RejectedDecision;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.ConditionPayload;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.DecisionPayload;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.ReasonPayload;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.ScopePayload;
import java.util.List;

/**
 * La decisión se publica con una sola forma para las dos variantes: una otorgada no trae motivos y una
 * rechazada no trae condiciones ni vigencia.
 */
final class DecisionPayloadDescriber implements CertificationDecisionDescriber<DecisionPayload> {

    @Override
    public DecisionPayload granted(GrantedDecision decision) {
        List<ConditionPayload> conditions = decision.conditions().stream()
                .map(condition -> new ConditionPayload(condition.findingId(), condition.criterionCode(),
                        condition.description(), condition.dueDate()))
                .toList();
        return payload(decision, List.of(), conditions, new ValidityFields(decision.validity()));
    }

    @Override
    public DecisionPayload rejected(RejectedDecision decision) {
        return payload(decision, new IneligibilityReasons().rejected(decision), List.of(), new ValidityFields());
    }

    private DecisionPayload payload(CertificationDecision decision, List<ReasonPayload> reasons,
                                    List<ConditionPayload> conditions, ValidityFields validity) {
        return new DecisionPayload(decision.outcome().name(), decision.isEligible(), reasons, conditions,
                validity.validFrom(), validity.validUntil(), decision.policyCode().value(),
                decision.jurisdictionCode().value(), decision.evaluatedAt(), ScopePayload.of(decision.scope()));
    }
}
