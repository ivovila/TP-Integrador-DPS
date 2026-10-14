package ar.edu.itba.certiflow.schema.model;

public final class OptionInListRuleKind implements ApprovalRuleKind {

    static final String NAME = "option-in-list";
    static final String OPTIONS = "options";

    @Override
    public boolean isNamed(String ruleKindName) {
        return NAME.equals(ruleKindName);
    }

    @Override
    public ApprovalRule<?> ruleFrom(RuleParameters parameters) {
        return new OptionInListRule(parameters.texts(OPTIONS));
    }
}
