package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.schema.model.RuleParameters;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidRuleConfigurationException;
import ar.edu.itba.certiflow.web.support.RequestFields;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Parámetros abiertos: cada tipo de regla declara y lee los suyos a través del catálogo. */
public final class RulePayload implements RuleParameters {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final Map<String, JsonNode> fields;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public RulePayload(Map<String, JsonNode> fields) {
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        RequestFields.require(type(), "rule.type");
    }

    static RulePayload of(String type, Map<String, Object> parameters) {
        Map<String, JsonNode> fields = new LinkedHashMap<>();
        fields.put("type", JSON.valueToTree(type));
        parameters.forEach((name, value) -> fields.put(name, JSON.valueToTree(value)));
        return new RulePayload(fields);
    }

    @JsonAnyGetter
    public Map<String, JsonNode> fields() { return fields; }

    public String type() { return text("type"); }

    private JsonNode required(String name) {
        JsonNode value = fields.get(name);
        if (value == null || value.isNull()) {
            throw new InvalidRuleConfigurationException("The rule needs a " + name);
        }
        return value;
    }

    @Override
    public String text(String name) {
        JsonNode value = required(name);
        if (!value.isString()) { throw new InvalidRuleConfigurationException(name + " must be text"); }
        return value.asString();
    }

    @Override
    public BigDecimal decimal(String name) {
        JsonNode value = required(name);
        if (!value.isNumber()) { throw new InvalidRuleConfigurationException(name + " must be numeric"); }
        return value.decimalValue();
    }

    @Override
    public Set<String> texts(String name) {
        JsonNode value = required(name);
        if (!value.isArray()) { throw new InvalidRuleConfigurationException(name + " must be an array"); }
        return value.valueStream().map(item -> {
            if (!item.isString()) { throw new InvalidRuleConfigurationException(name + " must contain text"); }
            return item.asString();
        }).collect(Collectors.toSet());
    }
}
