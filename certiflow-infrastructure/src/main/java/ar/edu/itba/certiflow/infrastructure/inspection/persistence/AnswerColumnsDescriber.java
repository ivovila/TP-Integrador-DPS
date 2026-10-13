package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.schema.model.AnswerDescriber;
import java.math.BigDecimal;

final class AnswerColumnsDescriber implements AnswerDescriber<AnswerColumns> {

    @Override
    public AnswerColumns number(BigDecimal value) {
        return new AnswerColumns("NUMBER", value.toPlainString());
    }

    @Override
    public AnswerColumns yesNo(boolean value) {
        return new AnswerColumns("YES_NO", String.valueOf(value));
    }

    @Override
    public AnswerColumns option(String option) {
        return new AnswerColumns("OPTION", option);
    }
}
