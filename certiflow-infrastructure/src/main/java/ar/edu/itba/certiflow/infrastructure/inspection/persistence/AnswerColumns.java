package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.schema.model.RawAnswer;
import java.math.BigDecimal;

record AnswerColumns(String type, String value) {

    static AnswerColumns of(RawAnswer raw) {
        return switch (raw) {
            case RawAnswer.Number(BigDecimal number) -> new AnswerColumns("NUMBER", number.toPlainString());
            case RawAnswer.YesNo(boolean yes) -> new AnswerColumns("YES_NO", Boolean.toString(yes));
            case RawAnswer.Option(String option) -> new AnswerColumns("OPTION", option);
        };
    }

    RawAnswer toRaw() {
        return switch (type) {
            case "NUMBER" -> new RawAnswer.Number(new BigDecimal(value));
            case "YES_NO" -> new RawAnswer.YesNo(Boolean.parseBoolean(value));
            case "OPTION" -> new RawAnswer.Option(value);
            default -> throw new IllegalStateException("Unknown answer type " + type);
        };
    }
}
