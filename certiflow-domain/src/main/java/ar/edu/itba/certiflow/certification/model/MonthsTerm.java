package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.certification.model.exceptions.InvalidValidityTermException;
import java.time.LocalDate;

public record MonthsTerm(int months) implements ValidityTerm {

    public MonthsTerm {
        if (months <= 0) {
            throw new InvalidValidityTermException(months);
        }
    }

    @Override
    public ValidityPeriod startingOn(LocalDate issuanceDate) {
        return new ValidityPeriod(issuanceDate, issuanceDate.plusMonths(months).minusDays(1));
    }
}
