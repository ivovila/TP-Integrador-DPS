package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.schema.model.AnswerTypeMismatchException;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import java.util.List;
import java.util.function.Predicate;
import tools.jackson.databind.JsonNode;

/** Traduce el valor JSON de una respuesta al texto que interpreta la regla del criterio. */
final class RawAnswers {

    private static final List<Predicate<JsonNode>> ACCEPTED_KINDS = List.of(
            JsonNode::isNumber, JsonNode::isBoolean, JsonNode::isString);

    private RawAnswers() {
    }

    static RawAnswer from(JsonNode node) {
        return ACCEPTED_KINDS.stream()
                .filter(acceptedKind -> node != null && acceptedKind.test(node))
                .findFirst()
                .map(acceptedKind -> new RawAnswer(node.asString()))
                .orElseThrow(() -> new AnswerTypeMismatchException("number, boolean or text",
                        new RawAnswer(String.valueOf(node))));
    }
}
