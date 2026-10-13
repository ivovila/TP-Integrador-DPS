package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Las revisiones de una inspección, numeradas desde 1 y en orden. La colección es dueña de la numeración:
 * nadie puede sellar una revisión con un número que no corresponda ni alterar la original.
 */
public record Revisions(List<Revision> revisions) {

    public Revisions {
        revisions = List.copyOf(revisions);
    }

    public static Revisions none() {
        return new Revisions(List.of());
    }

    /** Sella una revisión nueva con el número siguiente. La inspección queda cerrada con la primera. */
    public Revisions sealedWith(String reason, Evaluation evaluation, Stamp sealed) {
        Revision revision = new Revision(revisions.size() + 1, reason, evaluation, sealed);
        return new Revisions(Stream.concat(revisions.stream(), Stream.of(revision)).toList());
    }

    public boolean isEmpty() {
        return revisions.isEmpty();
    }

    public int size() {
        return revisions.size();
    }

    /** Lo que se firmó al cerrar: no cambia por más rectificaciones que haya. */
    public Revision original() {
        ensureSealed();
        return revisions.getFirst();
    }

    /** Cuándo se cerró la inspección, si ya se cerró: el sello de la primera revisión. */
    public Optional<Instant> firstSealedAt() {
        return revisions.stream().findFirst().map(Revision::sealedAt);
    }

    public Revision last() {
        ensureSealed();
        return revisions.getLast();
    }

    public List<Revision> asList() {
        return revisions;
    }

    private void ensureSealed() {
        if (revisions.isEmpty()) {
            throw new InspectionNotClosedException();
        }
    }
}
