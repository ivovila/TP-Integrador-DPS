package ar.edu.itba.certiflow.models.certificate.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;
import java.time.LocalDate;

public class InvalidValidityPeriodException extends DomainException {

    public InvalidValidityPeriodException(LocalDate validFrom, LocalDate validUntil) {
        super("A validity period cannot end (" + validUntil + ") before it starts (" + validFrom + ")");
    }
}
