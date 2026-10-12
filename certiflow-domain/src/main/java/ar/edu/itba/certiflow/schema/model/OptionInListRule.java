package ar.edu.itba.certiflow.schema.model;

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

    @Override
    public OptionAnswer answerFrom(RawAnswer raw) {
        if (raw instanceof RawAnswer.Option(String value)) {
            return new OptionAnswer(value);
        }
        throw new AnswerTypeMismatchException("option", raw);
    }
}
