package ar.edu.itba.certiflow.certification.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que una política va juntando mientras recorre los hallazgos. El resultado lo decide su estado, que solo
 * empeora: aprobado → condicional → rechazado.
 */
public final class PolicyAssessment {

    private final CertificationAssessmentContext context;
    private final String policyCode;
    private final ValidityTerm approvedTerm;
    private final ValidityTerm conditionalTerm;
    private final List<Ineligibility> reasons = new ArrayList<>();
    private final List<OpenNonConformity> conditioned = new ArrayList<>();
    private State state = State.APPROVED;

    PolicyAssessment(CertificationAssessmentContext context, String policyCode, ValidityTerm approvedTerm,
                     ValidityTerm conditionalTerm) {
        this.context = context;
        this.policyCode = policyCode;
        this.approvedTerm = approvedTerm;
        this.conditionalTerm = conditionalTerm;
    }

    public void block(IneligibilityReason reason, OpenNonConformity finding) {
        reasons.add(new Ineligibility(reason, "Open finding " + finding.criterionCode() + " severity " + finding.severity().label()));
        state = State.REJECTED;
    }

    public void requireCondition(OpenNonConformity finding) {
        conditioned.add(finding);
        state = state.withCondition();
    }

    CertificationDecision decision() {
        return state.decide(this);
    }

    private enum State {
        APPROVED {
            @Override
            CertificationDecision decide(PolicyAssessment assessment) {
                return CertificationDecision.approved(assessment.context, assessment.policyCode,
                        assessment.approvedTerm.startingOn(assessment.context.issuanceDate()));
            }
        },
        CONDITIONAL {
            @Override
            CertificationDecision decide(PolicyAssessment assessment) {
                ValidityPeriod validity = assessment.conditionalTerm.startingOn(assessment.context.issuanceDate());
                List<CertificationCondition> conditions = assessment.conditioned.stream()
                        .map(finding -> new CertificationCondition(finding.findingId(), finding.criterionCode(),
                                "Resolve and verify open nonconformity", validity.validUntil()))
                        .toList();
                return CertificationDecision.conditional(assessment.context, assessment.policyCode, validity, conditions);
            }
        },
        REJECTED {
            @Override
            State withCondition() {
                return this;
            }

            @Override
            CertificationDecision decide(PolicyAssessment assessment) {
                return CertificationDecision.rejected(assessment.context, assessment.policyCode, assessment.reasons);
            }
        };

        State withCondition() {
            return CONDITIONAL;
        }

        abstract CertificationDecision decide(PolicyAssessment assessment);
    }
}
