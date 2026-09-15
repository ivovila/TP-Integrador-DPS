package ar.edu.itba.certiflow.support;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.ports.Clock;

public class FixedClock implements Clock {

    private LocalDateTime now;

    public FixedClock(LocalDateTime now) {
        this.now = now;
    }

    @Override
    public LocalDateTime now() {
        return now;
    }

    public void advanceDays(long days) {
        now = now.plusDays(days);
    }
}
