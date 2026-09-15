package ar.edu.itba.certiflow.domain.model.certificate;

import java.time.LocalDate;

public record ValidityPeriod(LocalDate from, LocalDate until) {

    public ValidityPeriod {
        if (!until.isAfter(from)) {
            throw new IllegalArgumentException("La vigencia debe terminar despues de empezar");
        }
    }

    public static ValidityPeriod ofYears(LocalDate from, int years) {
        return new ValidityPeriod(from, from.plusYears(years));
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(until);
    }

    public boolean hasEndedBy(LocalDate date) {
        return date.isAfter(until);
    }
}
