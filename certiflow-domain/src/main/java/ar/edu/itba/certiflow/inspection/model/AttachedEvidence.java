package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;

public record AttachedEvidence(Evidence evidence, Stamp attached) {

    public Person attachedBy() {
        return attached.author();
    }

    public Instant attachedAt() {
        return attached.instant();
    }
}
