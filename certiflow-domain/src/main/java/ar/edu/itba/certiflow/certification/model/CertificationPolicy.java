package ar.edu.itba.certiflow.certification.model;

public interface CertificationPolicy {
    PolicyReference reference();
    CertificationDecision evaluate(CertificationAssessmentContext context);
}
