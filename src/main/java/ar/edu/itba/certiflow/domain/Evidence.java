package ar.edu.itba.certiflow.domain;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Reference and metadata only: binary storage is outside this module. */
public record Evidence(
        UUID id,
        Kind kind,
        URI reference,
        String description,
        String recordedBy,
        Instant recordedAt) {
    public enum Kind {
        PHOTO,
        DOCUMENT
    }

    public Evidence {
        Objects.requireNonNull(id);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(reference);
        Checks.require(reference.isAbsolute(), "Evidence needs an absolute reference");
        description = Checks.text(description, "description");
        recordedBy = Checks.text(recordedBy, "recordedBy");
        Objects.requireNonNull(recordedAt);
    }
}
