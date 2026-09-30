package ar.edu.itba.certiflow.models.finding;

public enum VerificationResult {
    ACCEPTED(true, false, FindingAudit.VERIFICATION_ACCEPTED),
    REJECTED(false, true, FindingAudit.VERIFICATION_REJECTED);

    private final boolean closesAction;
    private final boolean requiresNotes;
    private final FindingAudit audit;

    VerificationResult(boolean closesAction, boolean requiresNotes, FindingAudit audit) {
        this.closesAction = closesAction;
        this.requiresNotes = requiresNotes;
        this.audit = audit;
    }

    public boolean closesAction() {
        return closesAction;
    }

    public boolean requiresNotes() {
        return requiresNotes;
    }

    public FindingAudit audit() {
        return audit;
    }
}
