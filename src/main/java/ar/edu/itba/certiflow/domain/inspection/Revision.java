package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.Objects;

public record Revision(int number, String reason, Evaluation evaluation, Person by, Instant at) {
}
