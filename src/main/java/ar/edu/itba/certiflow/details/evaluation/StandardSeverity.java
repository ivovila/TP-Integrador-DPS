package ar.edu.itba.certiflow.details.evaluation;

import ar.edu.itba.certiflow.domain.evaluation.Outcome;
import ar.edu.itba.certiflow.domain.evaluation.Severity;

public enum StandardSeverity implements Severity {
    MINOR("Minor", Outcome.OBSERVED, false),
    MAJOR("Major", Outcome.REJECTED, true),
    CRITICAL("Critical", Outcome.REJECTED, true);

    private final String label;
    private final Outcome outcomeWhenUnmet;
    private final boolean blocksCertification;

    StandardSeverity(String label, Outcome outcomeWhenUnmet, boolean blocksCertification) {
        this.label = label;
        this.outcomeWhenUnmet = outcomeWhenUnmet;
        this.blocksCertification = blocksCertification;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public Outcome outcomeWhenUnmet() {
        return outcomeWhenUnmet;
    }

    @Override
    public boolean blocksCertification() {
        return blocksCertification;
    }
}
