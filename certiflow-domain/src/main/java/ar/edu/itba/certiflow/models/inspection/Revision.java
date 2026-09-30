package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public record Revision(int number, String reason, Evaluation evaluation, Person sealedBy, Instant sealedAt) {
}
