package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.shared.DomainException;
import java.time.LocalDate;

public class InvalidValidityPeriodException extends DomainException {

    public InvalidValidityPeriodException(LocalDate from, LocalDate to) {
        super("A validity period cannot end (" + to + ") before it starts (" + from + ")");
    }
}
