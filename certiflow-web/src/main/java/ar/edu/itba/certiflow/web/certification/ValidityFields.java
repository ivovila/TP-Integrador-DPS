package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
import java.time.LocalDate;

/** Los campos `validFrom` y `validUntil` del JSON de una decisión: viajan vacíos cuando la decisión no otorga nada. */
record ValidityFields(LocalDate validFrom, LocalDate validUntil) {

    ValidityFields(ValidityPeriod validity) {
        this(validity.validFrom(), validity.validUntil());
    }

    ValidityFields() {
        this(null, null);
    }
}
