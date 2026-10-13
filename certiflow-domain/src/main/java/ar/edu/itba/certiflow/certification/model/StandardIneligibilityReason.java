package ar.edu.itba.certiflow.certification.model;

public enum StandardIneligibilityReason implements IneligibilityReason {
    UNSUPPORTED_SEVERITY,
    MISSING_SUBSYSTEM,
    INVALID_PARTIAL_CERTIFICATE,
    INSPECTION_NOT_CLOSED,
    PARTIAL_INSPECTION,
    REQUIRES_PARTIAL_CERTIFICATES,
    BLOCKING_FINDINGS;

    @Override
    public String code() {
        return name();
    }
}
