package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public record GivenAnswer<A extends Answer>(A value, Person by, Instant at) {
}
