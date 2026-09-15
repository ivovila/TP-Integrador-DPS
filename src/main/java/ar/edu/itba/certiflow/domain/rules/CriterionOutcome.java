package ar.edu.itba.certiflow.domain.rules;

public enum CriterionOutcome {
    APPROVED,
    OBSERVED,
    REJECTED;

    public CriterionOutcome worst(CriterionOutcome other) {
        return compareTo(other) >= 0 ? this : other;
    }
}
