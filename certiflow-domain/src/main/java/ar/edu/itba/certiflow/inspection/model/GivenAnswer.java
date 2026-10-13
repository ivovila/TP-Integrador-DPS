package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;

public record GivenAnswer<A extends Answer>(A value, Stamp answered) {

    public Person answeredBy() {
        return answered.by();
    }

    public Instant answeredAt() {
        return answered.at();
    }
}
