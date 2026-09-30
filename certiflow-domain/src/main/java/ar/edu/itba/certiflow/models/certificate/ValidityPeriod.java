package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.certificate.exceptions.InvalidValidityPeriodException;
import java.time.LocalDate;

public record ValidityPeriod(LocalDate validFrom, LocalDate validUntil) {

    public ValidityPeriod {
        if (validUntil.isBefore(validFrom)) {
            throw new InvalidValidityPeriodException(validFrom, validUntil);
        }
    }

    public boolean covers(LocalDate referenceDate) {
        return !referenceDate.isBefore(validFrom) && !referenceDate.isAfter(validUntil);
    }
}
