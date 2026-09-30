package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.evaluation.Evidence;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public record AttachedEvidence(Evidence evidence, Person attachedBy, Instant attachedAt) {
}
