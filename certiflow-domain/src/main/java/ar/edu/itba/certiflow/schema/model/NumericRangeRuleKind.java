package ar.edu.itba.certiflow.schema.model;

public final class NumericRangeRuleKind implements ApprovalRuleKind {

    static final String NAME = "numeric-range";
    static final String UNIT = "unit";
    static final String MINIMUM = "minimum";
    static final String MAXIMUM = "maximum";

    @Override
    public boolean isNamed(String ruleKindName) {
        return NAME.equals(ruleKindName);
    }

    @Override
    public ApprovalRule<?> ruleFrom(RuleParameters parameters) {
        return new NumericRangeRule(new Unit(parameters.text(UNIT)),
                new Range(parameters.decimal(MINIMUM), parameters.decimal(MAXIMUM)));
    }
}
