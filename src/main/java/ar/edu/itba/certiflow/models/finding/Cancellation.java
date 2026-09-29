package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public record Cancellation(Person cancelledBy, Instant cancelledAt) {
}
