package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;

public record Cancellation(Person cancelledBy, Instant cancelledAt) {
}
