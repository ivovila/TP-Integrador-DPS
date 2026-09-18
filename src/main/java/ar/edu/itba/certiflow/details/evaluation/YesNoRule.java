package ar.edu.itba.certiflow.details.evaluation;

import ar.edu.itba.certiflow.domain.evaluation.ApprovalRule;

import java.util.Objects;

public record YesNoRule(YesNoAnswer expected) implements ApprovalRule<YesNoAnswer> {

    public YesNoRule {
        Objects.requireNonNull(expected, "expected");
    }

    @Override
    public boolean isSatisfiedBy(YesNoAnswer answer) {
        return expected == answer;
    }
}
