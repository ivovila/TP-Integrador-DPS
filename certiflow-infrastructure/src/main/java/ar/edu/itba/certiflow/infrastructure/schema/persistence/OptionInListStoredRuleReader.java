package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.OptionInListRule;
import org.springframework.stereotype.Component;

@Component
public class OptionInListStoredRuleReader implements StoredRuleReader {

    static final String RULE_TYPE = "option-in-list";

    @Override
    public boolean appliesTo(String ruleType) {
        return RULE_TYPE.equals(ruleType);
    }

    @Override
    public ApprovalRule<?> read(RuleDocument document) {
        return new OptionInListRule(document.options());
    }
}
