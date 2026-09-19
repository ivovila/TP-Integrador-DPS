package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public record AttachedEvidence(Evidence evidence, Person by, Instant at) {
}
