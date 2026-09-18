package ar.edu.itba.certiflow.domain.certificate;

import java.time.LocalDate;

public record ValidityPeriod(LocalDate from, LocalDate to) {

    public ValidityPeriod {
        if (to.isBefore(from)) {
            throw new InvalidValidityPeriodException(from, to);
        }
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
