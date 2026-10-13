package ar.edu.itba.certiflow.certification.model;

import java.time.LocalDate;

/** Cuánto dura lo que concede una política. Los plazos incluyen el día de emisión y el último día (S9). */
public interface ValidityTerm {

    ValidityPeriod startingOn(LocalDate from);

    static ValidityTerm months(int months) {
        requirePositive(months);
        return from -> new ValidityPeriod(from, from.plusMonths(months).minusDays(1));
    }

    static ValidityTerm days(int days) {
        requirePositive(days);
        return from -> new ValidityPeriod(from, from.plusDays(days - 1));
    }

    /** Para una política que nunca concede esa modalidad. */
    static ValidityTerm notGranted() {
        return from -> {
            throw new IllegalStateException("This policy grants no validity for that outcome");
        };
    }

    private static void requirePositive(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("A validity term must be positive");
        }
    }
}
