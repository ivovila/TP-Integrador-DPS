package ar.edu.itba.certiflow.certification.model;

public interface CertificationPolicy {
    String code();
    CertificationDecision evaluate(CertificationAssessmentContext context);
}
