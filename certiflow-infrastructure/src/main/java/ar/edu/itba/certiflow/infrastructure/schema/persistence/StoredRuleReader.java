package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.schema.model.ApprovalRule;

/** Lee un tipo de regla guardada. Un tipo de regla nuevo es una implementación más. */
public interface StoredRuleReader {

    boolean appliesTo(String ruleType);

    ApprovalRule<?> read(RuleDocument document);
}
