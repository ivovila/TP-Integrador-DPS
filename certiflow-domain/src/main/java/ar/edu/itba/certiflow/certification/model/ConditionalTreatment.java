package ar.edu.itba.certiflow.certification.model;

/** La política acepta el hallazgo a condición de resolverlo dentro del plazo que concede. */
public record ConditionalTreatment(ValidityTerm conditionalTerm) implements FindingTreatment {

    @Override
    public void applyTo(OpenNonConformity finding, PolicyAssessment assessment) {
        assessment.requireCondition(finding, conditionalTerm);
    }
}
