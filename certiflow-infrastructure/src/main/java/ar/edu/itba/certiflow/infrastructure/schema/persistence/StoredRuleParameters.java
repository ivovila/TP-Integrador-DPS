package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.UnreadableStoredDataException;
import ar.edu.itba.certiflow.schema.model.RuleParameters;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

/**
 * Una regla como está guardada. Las filas anteriores traen además, vacíos, los parámetros de los otros tipos
 * de regla: un parámetro vacío es lo mismo que uno que no está.
 */
record StoredRuleParameters(Map<String, JsonNode> storedRule) implements RuleParameters {

    static final String RULE_KIND = "type";

    String ruleKindName() {
        return text(RULE_KIND);
    }

    @Override
    public String text(String parameterName) {
        return stored(parameterName).asString();
    }

    @Override
    public BigDecimal decimal(String parameterName) {
        return stored(parameterName).decimalValue();
    }

    @Override
    public Set<String> texts(String parameterName) {
        return stored(parameterName).valueStream().map(JsonNode::asString).collect(Collectors.toSet());
    }

    private JsonNode stored(String parameterName) {
        return Optional.ofNullable(storedRule.get(parameterName))
                .filter(storedValue -> !storedValue.isNull())
                .orElseThrow(() -> new UnreadableStoredDataException("rule parameter", parameterName));
    }
}
