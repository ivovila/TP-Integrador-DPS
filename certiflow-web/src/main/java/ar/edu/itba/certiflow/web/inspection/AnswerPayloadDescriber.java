package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.schema.model.AnswerDescriber;
import java.math.BigDecimal;

final class AnswerPayloadDescriber implements AnswerDescriber<AnswerPayload> {

    @Override
    public AnswerPayload number(BigDecimal value) {
        return new NumberAnswerPayload(value);
    }

    @Override
    public AnswerPayload yesNo(boolean value) {
        return new YesNoAnswerPayload(value);
    }

    @Override
    public AnswerPayload option(String option) {
        return new OptionAnswerPayload(option);
    }
}
