package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.Objects;

/**
 * Versión sellada del acta. La número 1 nace al cerrar la inspección; cada rectificación agrega
 * otra y ninguna se modifica, de modo que la revisión original se conserva siempre.
 */
public record Revision(int number, String reason, Evaluation evaluation, Person by, Instant at) {

    public Revision {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(evaluation, "evaluation");
        Objects.requireNonNull(by, "by");
        Objects.requireNonNull(at, "at");
    }
}
