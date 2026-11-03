package ar.edu.itba.certiflow.certification.model;

public interface GlobalCertificationPolicy {

    PolicyReference reference();

    CertificationDecision evaluate(GlobalAssessmentContext context);
}
