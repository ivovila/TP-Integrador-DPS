package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public record GivenAnswer<A extends Answer>(A value, Person answeredBy, Instant answeredAt) {
}
