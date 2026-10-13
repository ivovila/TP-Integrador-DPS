package ar.edu.itba.certiflow.shared.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * La precisión con la que se guardan los instantes (microsegundos, como PostgreSQL). Se trunca y no se redondea:
 * un instante normalizado nunca queda después del real, así una lectura posterior del reloj no puede caer antes
 * de una vigencia que acaba de empezar.
 */
public final class StoredInstant {
    private StoredInstant() { }

    public static Instant normalize(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }
}
