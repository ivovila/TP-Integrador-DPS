package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.Unit;
import org.springframework.stereotype.Component;

@Component
public class NumericRangeStoredRuleReader implements StoredRuleReader {

    static final String RULE_TYPE = "numeric-range";

    @Override
    public boolean appliesTo(String ruleType) {
        return RULE_TYPE.equals(ruleType);
    }

    @Override
    public ApprovalRule<?> read(RuleDocument document) {
        return new NumericRangeRule(new Unit(document.unit()), new Range(document.minimum(), document.maximum()));
    }
}
