package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.UnreadableStoredDataException;
import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import java.util.Arrays;
import org.springframework.stereotype.Component;

@Component
public class YesNoStoredRuleReader implements StoredRuleReader {

    static final String RULE_TYPE = "yes-no";

    @Override
    public boolean appliesTo(String ruleType) {
        return RULE_TYPE.equals(ruleType);
    }

    @Override
    public ApprovalRule<?> read(RuleDocument document) {
        YesNoAnswer expectedAnswer = Arrays.stream(YesNoAnswer.values())
                .filter(candidate -> candidate.name().equals(document.expected()))
                .findFirst()
                .orElseThrow(() -> new UnreadableStoredDataException("expected yes/no answer", document.expected()));
        return new YesNoRule(expectedAnswer);
    }
}
