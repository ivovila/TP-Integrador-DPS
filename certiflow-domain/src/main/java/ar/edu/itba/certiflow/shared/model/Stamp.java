package ar.edu.itba.certiflow.shared.model;

import java.time.Instant;

/** Quién y cuándo: la firma que deja cada hecho del dominio. */
public record Stamp(Person author, Instant instant) {
}
