package ar.edu.itba.certiflow.domain.model.finding;

public enum Severity {
    MINOR,
    MAJOR,
    CRITICAL;

    public boolean isAtLeast(Severity other) {
        return compareTo(other) >= 0;
    }
}
