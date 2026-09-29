package ar.edu.itba.certiflow.models.finding;

public enum VerificationResult {
    ACCEPTED(true, false),
    REJECTED(false, true);

    private final boolean closesAction;
    private final boolean requiresNotes;

    VerificationResult(boolean closesAction, boolean requiresNotes) {
        this.closesAction = closesAction;
        this.requiresNotes = requiresNotes;
    }

    public boolean closesAction() {
        return closesAction;
    }

    public boolean requiresNotes() {
        return requiresNotes;
    }
}
