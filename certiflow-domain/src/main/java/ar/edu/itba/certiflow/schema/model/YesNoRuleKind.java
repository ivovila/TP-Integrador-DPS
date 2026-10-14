package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.schema.model.exceptions.InvalidRuleConfigurationException;
import java.util.Arrays;

public final class YesNoRuleKind implements ApprovalRuleKind {

    static final String NAME = "yes-no";
    static final String EXPECTED = "expected";

    @Override
    public boolean isNamed(String ruleKindName) {
        return NAME.equals(ruleKindName);
    }

    @Override
    public ApprovalRule<?> ruleFrom(RuleParameters parameters) {
        String expectedAnswerName = parameters.text(EXPECTED);
        return new YesNoRule(Arrays.stream(YesNoAnswer.values())
                .filter(candidate -> candidate.name().equals(expectedAnswerName))
                .findFirst()
                .orElseThrow(() -> new InvalidRuleConfigurationException(
                        "A yes/no rule expects YES or NO, not " + expectedAnswerName)));
    }
}
