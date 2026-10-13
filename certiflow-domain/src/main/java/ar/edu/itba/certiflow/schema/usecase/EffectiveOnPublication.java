package ar.edu.itba.certiflow.schema.usecase;

import java.time.Instant;

public record EffectiveOnPublication() implements RequestedEffectiveDate {

    @Override
    public Instant resolveAt(Instant publishedAt) {
        return publishedAt;
    }
}
