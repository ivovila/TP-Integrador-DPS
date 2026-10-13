package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.certification.model.exceptions.InvalidValidityTermException;
import java.time.LocalDate;

public record DaysTerm(int days) implements ValidityTerm {

    public DaysTerm {
        if (days <= 0) {
            throw new InvalidValidityTermException(days);
        }
    }

    @Override
    public ValidityPeriod startingOn(LocalDate issuanceDate) {
        return new ValidityPeriod(issuanceDate, issuanceDate.plusDays(days - 1));
    }
}
