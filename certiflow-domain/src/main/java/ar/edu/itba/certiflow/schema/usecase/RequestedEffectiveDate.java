package ar.edu.itba.certiflow.schema.usecase;

import java.time.Instant;

/** Desde cuándo se pide que rija una versión: una fecha dada, o el momento en que se publica. */
public interface RequestedEffectiveDate {

    Instant resolveAt(Instant publishedAt);
}
