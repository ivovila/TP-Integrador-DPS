package ar.edu.itba.certiflow.schema.model;



public enum StandardSeverity implements Severity {
    MINOR("Minor", Outcome.OBSERVED),
    MAJOR("Major", Outcome.REJECTED),
    CRITICAL("Critical", Outcome.REJECTED);

    private final String label;
    private final Outcome outcomeWhenUnmet;

    StandardSeverity(String label, Outcome outcomeWhenUnmet) {
        this.label = label;
        this.outcomeWhenUnmet = outcomeWhenUnmet;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public Outcome outcomeWhenUnmet() {
        return outcomeWhenUnmet;
    }

}
