package ar.edu.itba.certiflow.models.finding;

public enum VerificationResult {
    ACCEPTED(true),
    REJECTED(false);

    private final boolean closesAction;

    VerificationResult(boolean closesAction) {
        this.closesAction = closesAction;
    }

    public boolean closesAction() {
        return closesAction;
    }
}
