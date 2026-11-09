package ar.edu.itba.certiflow.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderedClockTest {
    @Test
    void aWallClockRollbackDoesNotDateAnInspectionBeforeItsSchemaPublication() {
        var moments = new ArrayDeque<>(List.of(Instant.parse("2026-11-09T14:42:02.168676900Z"),
                Instant.parse("2026-11-09T14:42:01.641297Z"), Instant.parse("2026-11-09T14:42:03.123456789Z")));
        Clock source = new Clock() {
            public Instant instant() { return moments.removeFirst(); }
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { throw new UnsupportedOperationException(); }
        };
        Clock clock = new OrderedClock(source);
        assertEquals(Instant.parse("2026-11-09T14:42:02.168676Z"), clock.instant());
        assertEquals(Instant.parse("2026-11-09T14:42:02.168677Z"), clock.instant());
        assertEquals(Instant.parse("2026-11-09T14:42:03.123456Z"), clock.instant());
    }

    @Test
    void simultaneousTimestampsAndDifferentZonesShareTheSameOrder() {
        Instant moment = Instant.parse("2026-11-09T14:42:02.168676900Z");
        Clock clock = new OrderedClock(Clock.fixed(moment, ZoneOffset.UTC));
        assertEquals(Instant.parse("2026-11-09T14:42:02.168676Z"), clock.instant());
        Clock local = clock.withZone(ZoneId.of("America/Argentina/Buenos_Aires"));
        assertEquals(Instant.parse("2026-11-09T14:42:02.168677Z"), local.instant());
        assertEquals(Instant.parse("2026-11-09T14:42:02.168678Z"), clock.instant());
        assertEquals(ZoneId.of("America/Argentina/Buenos_Aires"), local.getZone());
    }
}
