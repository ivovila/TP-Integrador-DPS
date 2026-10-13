package ar.edu.itba.certiflow.web.inspection;

import com.fasterxml.jackson.annotation.JsonValue;

public record YesNoAnswerPayload(@JsonValue boolean value) implements AnswerPayload {
}
