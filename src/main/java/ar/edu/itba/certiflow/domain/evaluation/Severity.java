package ar.edu.itba.certiflow.domain.evaluation;

public interface Severity {

    String label();

    Outcome outcomeWhenUnmet();

    boolean blocksCertification();
}
