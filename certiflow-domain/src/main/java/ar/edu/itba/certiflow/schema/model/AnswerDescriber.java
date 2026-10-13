package ar.edu.itba.certiflow.schema.model;

import java.math.BigDecimal;

public interface AnswerDescriber<T> {

    T number(BigDecimal value);

    T yesNo(boolean value);

    T option(String option);
}
