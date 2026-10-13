package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.schema.model.AnswerTypeMismatchException;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import tools.jackson.databind.JsonNode;

/** Traduce el valor JSON de una respuesta a un valor de Java; qué significa lo decide la regla del criterio. */
final class RawAnswers {

    private static final List<Map.Entry<Predicate<JsonNode>, Function<JsonNode, Object>>> READERS = List.of(
            Map.entry(JsonNode::isNumber, JsonNode::decimalValue),
            Map.entry(JsonNode::isBoolean, JsonNode::booleanValue),
            Map.entry(JsonNode::isString, JsonNode::stringValue));

    private RawAnswers() {
    }

    static RawAnswer from(JsonNode node) {
        return READERS.stream()
                .filter(reader -> node != null && reader.getKey().test(node))
                .findFirst()
                .map(reader -> new RawAnswer(reader.getValue().apply(node)))
                .orElseThrow(() -> new AnswerTypeMismatchException("number, boolean or text",
                        new RawAnswer(String.valueOf(node))));
    }
}
