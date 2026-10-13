package ar.edu.itba.certiflow.certification.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lo que una política va juntando mientras recorre los hallazgos. El resultado lo decide su estado, que solo
 * empeora: aprobado → condicional → rechazado.
 */
public final class PolicyAssessment {

    private final CertificationAssessmentContext context;
    private final String policyCode;
    private final ValidityTerm approvedTerm;
    private final List<Ineligibility> reasons = new ArrayList<>();
    private final List<OpenNonConformity> conditioned = new ArrayList<>();
    private final List<ValidityTerm> conditionalTerms = new ArrayList<>();
    private State state = State.APPROVED;

    PolicyAssessment(CertificationAssessmentContext context, String policyCode, ValidityTerm approvedTerm) {
        this.context = context;
        this.policyCode = policyCode;
        this.approvedTerm = approvedTerm;
    }

    public void block(IneligibilityReason reason, OpenNonConformity finding) {
        reasons.add(new Ineligibility(reason, "Open finding " + finding.criterionCode() + " severity " + finding.severity().label()));
        state = State.REJECTED;
    }

    public void requireCondition(OpenNonConformity finding, ValidityTerm conditionalTerm) {
        conditioned.add(finding);
        conditionalTerms.add(conditionalTerm);
        state = state.withCondition();
    }

    CertificationDecision decision() {
        return state.decide(this);
    }

    /** Con varios hallazgos condicionados rige el plazo que vence primero. */
    private ValidityPeriod shortestConditionalValidity() {
        List<ValidityPeriod> conditionalValidities = conditionalTerms.stream()
                .map(conditionalTerm -> conditionalTerm.startingOn(context.issuanceDate()))
                .toList();
        return Collections.min(conditionalValidities, Comparator.comparing(ValidityPeriod::validUntil));
    }

    private enum State {
        APPROVED {
            @Override
            CertificationDecision decide(PolicyAssessment assessment) {
                return new GrantedDecision(List.of(), assessment.approvedTerm.startingOn(assessment.context.issuanceDate()),
                        assessment.policyCode, assessment.context.jurisdictionCode(), assessment.context.evaluatedAt(),
                        assessment.context.result().scope());
            }
        },
        CONDITIONAL {
            @Override
            CertificationDecision decide(PolicyAssessment assessment) {
                ValidityPeriod validity = assessment.shortestConditionalValidity();
                List<CertificationCondition> conditions = assessment.conditioned.stream()
                        .map(finding -> new CertificationCondition(finding.findingId(), finding.criterionCode(),
                                "Resolve and verify open nonconformity", validity.validUntil()))
                        .toList();
                return new GrantedDecision(conditions, validity, assessment.policyCode, assessment.context.jurisdictionCode(),
                        assessment.context.evaluatedAt(), assessment.context.result().scope());
            }
        },
        REJECTED {
            @Override
            State withCondition() {
                return this;
            }

            @Override
            CertificationDecision decide(PolicyAssessment assessment) {
                return new RejectedDecision(assessment.reasons, assessment.policyCode, assessment.context.jurisdictionCode(),
                        assessment.context.evaluatedAt(), assessment.context.result().scope());
            }
        };

        State withCondition() {
            return CONDITIONAL;
        }

        abstract CertificationDecision decide(PolicyAssessment assessment);
    }
}
