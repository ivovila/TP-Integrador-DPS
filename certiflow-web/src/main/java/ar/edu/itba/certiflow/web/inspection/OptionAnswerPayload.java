package ar.edu.itba.certiflow.web.inspection;

import com.fasterxml.jackson.annotation.JsonValue;

public record OptionAnswerPayload(@JsonValue String value) implements AnswerPayload {
}
