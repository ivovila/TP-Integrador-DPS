package ar.edu.itba.certiflow.domain.evaluation;


import java.util.Objects;

public record YesNoRule(YesNoAnswer expected) implements ApprovalRule<YesNoAnswer> {

    @Override
    public boolean isSatisfiedBy(YesNoAnswer answer) {
        return expected == answer;
    }
}
