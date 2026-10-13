package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.schema.model.ApprovalRuleDescriber;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.Unit;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import java.util.Set;
import java.util.TreeSet;

final class RuleDocumentDescriber implements ApprovalRuleDescriber<RuleDocument> {

    @Override
    public RuleDocument numericRange(Unit unit, Range range) {
        return new RuleDocument(NumericRangeStoredRuleReader.RULE_TYPE, unit.symbol(), range.minimum(), range.maximum(),
                null, null);
    }

    @Override
    public RuleDocument yesNo(YesNoAnswer expectedAnswer) {
        return new RuleDocument(YesNoStoredRuleReader.RULE_TYPE, null, null, null, expectedAnswer.name(), null);
    }

    @Override
    public RuleDocument optionInList(Set<String> acceptedOptions) {
        return new RuleDocument(OptionInListStoredRuleReader.RULE_TYPE, null, null, null, null,
                new TreeSet<>(acceptedOptions));
    }
}
