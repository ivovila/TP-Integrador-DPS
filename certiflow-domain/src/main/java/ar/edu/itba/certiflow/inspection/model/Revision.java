package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;

public record Revision(int number, String reason, Evaluation evaluation, Stamp sealed) {

    public Person sealedBy() {
        return sealed.by();
    }

    public Instant sealedAt() {
        return sealed.at();
    }
}
