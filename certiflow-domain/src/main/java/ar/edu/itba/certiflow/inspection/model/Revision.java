package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;

public record Revision(int number, String reason, Evaluation evaluation, Person sealedBy, Instant sealedAt) {
}
