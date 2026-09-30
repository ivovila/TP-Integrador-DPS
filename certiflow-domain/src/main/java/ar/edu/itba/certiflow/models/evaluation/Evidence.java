package ar.edu.itba.certiflow.models.evaluation;

import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;

public record Evidence(EvidenceKind kind, String reference, String description) {

    public Evidence {
        if (reference.isBlank()) {
            throw new RequiredValueMissingException("An evidence must reference its file");
        }
    }

    public boolean isOfKind(EvidenceKind expectedKind) {
        return kind.equals(expectedKind);
    }

    public boolean isSameFileAs(Evidence other) {
        return reference.equals(other.reference);
    }
}
