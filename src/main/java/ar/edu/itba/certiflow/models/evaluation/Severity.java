package ar.edu.itba.certiflow.models.evaluation;

public interface Severity {

    String label();

    Outcome outcomeWhenUnmet();

    boolean blocksCertification();
}
