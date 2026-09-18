package ar.edu.itba.certiflow.domain.evaluation;

public record Evidence(EvidenceKind kind, String reference, String description) {

    public Evidence {
        if (reference.isBlank()) {
            throw new IllegalArgumentException("An evidence must reference its file");
        }
    }

    public boolean isOfKind(EvidenceKind other) {
        return kind.equals(other);
    }
}
