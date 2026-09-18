package ar.edu.itba.certiflow.details.evaluation;

import ar.edu.itba.certiflow.domain.evaluation.ApprovalRule;
import ar.edu.itba.certiflow.domain.evaluation.InvalidRuleConfigurationException;

import java.util.Set;

public record OptionInListRule(Set<String> acceptedOptions) implements ApprovalRule<OptionAnswer> {

    public OptionInListRule {
        acceptedOptions = Set.copyOf(acceptedOptions);
        if (acceptedOptions.isEmpty()) {
            throw new InvalidRuleConfigurationException("An option rule needs at least one accepted option");
        }
    }

    @Override
    public boolean isSatisfiedBy(OptionAnswer answer) {
        return acceptedOptions.contains(answer.option());
    }
}
