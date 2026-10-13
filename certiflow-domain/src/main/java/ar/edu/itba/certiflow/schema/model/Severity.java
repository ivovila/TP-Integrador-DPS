package ar.edu.itba.certiflow.schema.model;



public interface Severity {

    String label();

    Outcome outcomeWhenUnmet();

}
