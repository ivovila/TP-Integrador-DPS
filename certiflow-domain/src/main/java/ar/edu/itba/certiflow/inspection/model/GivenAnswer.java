package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;

public record GivenAnswer<A extends Answer>(A value, Person answeredBy, Instant answeredAt) {
}
