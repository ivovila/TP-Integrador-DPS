package ar.edu.itba.certiflow.web.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sellos con la precisión de PostgreSQL, ordenados aun cuando el reloj del sistema retrocede.
 * Sigue la hora del sistema cuando avanza; durante un retroceso asigna el siguiente microsegundo.
 * El orden es local al proceso: los servidores de un despliegue siguen necesitando relojes sincronizados.
 */
final class OrderedClock extends Clock {
    private final Clock source;
    private final AtomicReference<Instant> previous;

    OrderedClock(Clock source) {
        this(source, new AtomicReference<>(Instant.MIN));
    }

    private OrderedClock(Clock source, AtomicReference<Instant> previous) {
        this.source = source;
        this.previous = previous;
    }

    @Override
    public Instant instant() {
        Instant observed = source.instant().truncatedTo(ChronoUnit.MICROS);
        return previous.updateAndGet(last -> observed.isAfter(last) ? observed : last.plus(1, ChronoUnit.MICROS));
    }

    @Override
    public ZoneId getZone() { return source.getZone(); }

    @Override
    public Clock withZone(ZoneId zone) { return new OrderedClock(source.withZone(zone), previous); }
}
