package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public record Revision(int number, String reason, Evaluation evaluation, Person by, Instant at) {
}
