package ar.edu.itba.certiflow.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SharedConfigurationTest {

    /** Nada vuelve a normalizar los instantes: si el reloj entregara nanosegundos, lo guardado no coincidiría con lo leído. */
    @Test
    void theSystemClockGivesInstantsWithThePrecisionThatIsStored() {
        Clock clock = new SharedConfiguration().clock();

        Stream.generate(clock::instant).limit(1_000)
                .forEach(instant -> assertEquals(instant.truncatedTo(ChronoUnit.MICROS), instant));
    }

    @Test
    void theClockTruncatesInsteadOfRounding() {
        Instant real = Instant.parse("2026-10-13T15:52:46.849756900Z");
        Clock clock = Clock.tick(Clock.fixed(real, java.time.ZoneOffset.UTC), java.time.Duration.ofNanos(1_000));

        assertEquals(Instant.parse("2026-10-13T15:52:46.849756Z"), clock.instant());
    }
}
