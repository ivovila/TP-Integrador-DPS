package ar.edu.itba.certiflow.schema.model;

import java.math.BigDecimal;

/** Una respuesta escrita como el texto que su regla sabe volver a leer. */
public final class AnswerText implements AnswerDescriber<String> {

    @Override
    public String number(BigDecimal value) {
        return value.toPlainString();
    }

    @Override
    public String yesNo(boolean value) {
        return String.valueOf(value);
    }

    @Override
    public String option(String option) {
        return option;
    }
}
