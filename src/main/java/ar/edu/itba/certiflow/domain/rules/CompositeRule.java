package ar.edu.itba.certiflow.domain.rules;

import java.util.List;

import ar.edu.itba.certiflow.domain.model.shared.Measurement;
import ar.edu.itba.certiflow.domain.model.shared.Response;

public record CompositeRule(List<CriterionRule> rules) implements CriterionRule {

    public CompositeRule {
        rules = List.copyOf(rules);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("Una regla compuesta necesita al menos una regla");
        }
    }

    public static CompositeRule allOf(CriterionRule... rules) {
        return new CompositeRule(List.of(rules));
    }

    @Override
    public CriterionOutcome evaluate(Response response) {
        return rules.stream()
                .map(rule -> rule.evaluate(response))
                .reduce(CriterionOutcome.APPROVED, CriterionOutcome::worst);
    }

    @Override
    public boolean accepts(Measurement measurement) {
        return rules.stream().anyMatch(rule -> rule.accepts(measurement));
    }
}
