package ar.edu.itba.certiflow.certification.model;

import java.time.LocalDate;

/** Cuánto dura lo que concede una política. Los plazos incluyen el día de emisión y el último día (S9). */
public interface ValidityTerm {

    ValidityPeriod startingOn(LocalDate issuanceDate);
}
