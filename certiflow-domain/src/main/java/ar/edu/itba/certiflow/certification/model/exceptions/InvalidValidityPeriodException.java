package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;
import java.time.LocalDate;

public class InvalidValidityPeriodException extends InvalidValueException {

    public InvalidValidityPeriodException(LocalDate validFrom, LocalDate validUntil) {
        super("A validity period cannot end (" + validUntil + ") before it starts (" + validFrom + ")");
    }
}
