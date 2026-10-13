package ar.edu.itba.certiflow.web.inspection;

import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;

public record NumberAnswerPayload(@JsonValue BigDecimal value) implements AnswerPayload {
}
