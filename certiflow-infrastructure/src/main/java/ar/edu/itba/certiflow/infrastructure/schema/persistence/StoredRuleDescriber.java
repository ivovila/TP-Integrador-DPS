package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.schema.model.RuleDescriber;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Una regla se guarda como un objeto JSON con su tipo y sus parámetros, sean los que sean: así un tipo de
 * regla nuevo se guarda sin tocar esta clase. Las opciones van ordenadas para que la misma regla se escriba
 * siempre igual.
 */
final class StoredRuleDescriber implements RuleDescriber<Map<String, JsonNode>> {

    private final JsonNodeFactory nodes = JsonNodeFactory.instance;
    private final Map<String, JsonNode> parameters = new LinkedHashMap<>();

    @Override
    public RuleDescriber<Map<String, JsonNode>> text(String parameterName, String value) {
        parameters.put(parameterName, nodes.stringNode(value));
        return this;
    }

    @Override
    public RuleDescriber<Map<String, JsonNode>> decimal(String parameterName, BigDecimal value) {
        parameters.put(parameterName, nodes.numberNode(value));
        return this;
    }

    @Override
    public RuleDescriber<Map<String, JsonNode>> texts(String parameterName, Set<String> values) {
        ArrayNode storedValues = nodes.arrayNode();
        new TreeSet<>(values).forEach(storedValues::add);
        parameters.put(parameterName, storedValues);
        return this;
    }

    @Override
    public Map<String, JsonNode> ofKind(String ruleKindName) {
        Map<String, JsonNode> storedRule = new LinkedHashMap<>();
        storedRule.put(StoredRuleParameters.RULE_KIND, nodes.stringNode(ruleKindName));
        storedRule.putAll(parameters);
        return storedRule;
    }
}
