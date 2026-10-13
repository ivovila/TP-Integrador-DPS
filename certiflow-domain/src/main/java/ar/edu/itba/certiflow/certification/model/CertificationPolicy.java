package ar.edu.itba.certiflow.certification.model;

public interface CertificationPolicy {
    PolicyCode code();
    CertificationDecision evaluate(CertificationAssessmentContext context);
}
