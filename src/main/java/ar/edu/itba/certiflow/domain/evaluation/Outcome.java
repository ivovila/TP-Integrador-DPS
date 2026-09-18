package ar.edu.itba.certiflow.domain.evaluation;

/** Vocabulario cerrado por el enunciado; PENDING cubre al criterio que todavía no puede evaluarse. */
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
