package ar.edu.itba.certiflow.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class MutableClock extends Clock {

    private Instant currentInstant;

    public MutableClock(Instant startInstant) {
        this.currentInstant = startInstant;
    }

    public void advanceDays(long numberOfDays) {
        currentInstant = currentInstant.plus(Duration.ofDays(numberOfDays));
    }

    public void moveTo(LocalDate targetDate) {
        currentInstant = targetDate.atTime(9, 0).toInstant(ZoneOffset.UTC);
    }

    @Override
    public Instant instant() {
        return currentInstant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
