package ar.edu.itba.certiflow.certification.model;

public interface CertificationDecisionDescriber<T> {

    T granted(GrantedDecision decision);

    T rejected(RejectedDecision decision);
}
