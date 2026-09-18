package ar.edu.itba.certiflow.domain.certificate;

import java.time.LocalDate;
import java.util.Objects;

/** Ambos extremos incluidos: el certificado sigue vigente durante todo el día de vencimiento. */
public record ValidityPeriod(LocalDate from, LocalDate to) {

    public ValidityPeriod {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new InvalidValidityPeriodException(from, to);
        }
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
