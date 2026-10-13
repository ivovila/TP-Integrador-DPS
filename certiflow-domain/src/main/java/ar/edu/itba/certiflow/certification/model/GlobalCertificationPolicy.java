package ar.edu.itba.certiflow.certification.model;

public interface GlobalCertificationPolicy {

    PolicyCode code();

    CertificationDecision evaluate(GlobalAssessmentContext context);
}
