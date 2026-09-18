package ar.edu.itba.certiflow.domain.evaluation;

import java.util.Objects;

/** El dominio no guarda binarios: reference es la ubicación del archivo adjunto. */
public record Evidence(EvidenceKind kind, String reference, String description) {

    public Evidence {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(description, "description");
        if (reference.isBlank()) {
            throw new IllegalArgumentException("An evidence must reference its file");
        }
    }

    public boolean isOfKind(EvidenceKind other) {
        return kind.equals(other);
    }
}
