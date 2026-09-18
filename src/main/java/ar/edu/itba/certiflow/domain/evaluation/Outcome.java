package ar.edu.itba.certiflow.domain.evaluation;

public enum Outcome {
    PENDING(false),
    APPROVED(false),
    OBSERVED(true),
    REJECTED(true);

    private final boolean raisesFinding;

    Outcome(boolean raisesFinding) {
        this.raisesFinding = raisesFinding;
    }

    public boolean raisesFinding() {
        return raisesFinding;
    }
}
