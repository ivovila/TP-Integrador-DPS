package ar.edu.itba.certiflow.certification.model;

public interface GlobalCertificationPolicy {

    String code();

    CertificationDecision evaluate(GlobalAssessmentContext context);
}
