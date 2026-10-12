package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;

public record AttachedEvidence(Evidence evidence, Person attachedBy, Instant attachedAt) {
}
