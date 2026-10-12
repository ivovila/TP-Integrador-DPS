package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.schema.model.AnswerTypeMismatchException;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import java.math.BigDecimal;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

final class RawAnswers {

    private RawAnswers() {
    }

    static RawAnswer from(JsonNode value) {
        if (value != null && value.isNumber()) {
            return new RawAnswer.Number(value.decimalValue());
        }
        if (value != null && value.isBoolean()) {
            return new RawAnswer.YesNo(value.booleanValue());
        }
        if (value != null && value.isString()) {
            return new RawAnswer.Option(value.stringValue());
        }
        throw new AnswerTypeMismatchException("number, boolean or text", new RawAnswer.Option(String.valueOf(value)));
    }

    static JsonNode toJson(RawAnswer raw) {
        return switch (raw) {
            case RawAnswer.Number(BigDecimal number) -> JsonNodeFactory.instance.numberNode(number);
            case RawAnswer.YesNo(boolean yes) -> JsonNodeFactory.instance.booleanNode(yes);
            case RawAnswer.Option(String option) -> JsonNodeFactory.instance.stringNode(option);
        };
    }
}
