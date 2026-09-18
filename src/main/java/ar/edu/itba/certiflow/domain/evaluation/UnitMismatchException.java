package ar.edu.itba.certiflow.domain.evaluation;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class UnitMismatchException extends DomainException {

    public UnitMismatchException(Unit expected, Unit actual) {
        super("Expected a measurement in " + expected.symbol() + " but it was recorded in " + actual.symbol());
    }
}
