package ar.edu.itba.certiflow.schema.model;



public interface Severity {

    /** Identificador estable con el que se guarda y se publica. */
    String code();

    String label();

    Outcome outcomeWhenUnmet();

}
