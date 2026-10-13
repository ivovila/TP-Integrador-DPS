package ar.edu.itba.certiflow.schema.usecase;

import java.time.Instant;

public record EffectiveFrom(Instant effectiveFrom) implements RequestedEffectiveDate {

    @Override
    public Instant resolveAt(Instant publishedAt) {
        return effectiveFrom;
    }
}
