package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.web.support.RequestFields;
import java.math.BigDecimal;
import java.util.Set;

public record RulePayload(String type, String unit, BigDecimal minimum, BigDecimal maximum, String expected, Set<String> options) {

    public RulePayload {
        RequestFields.require(type, "rule.type");
    }
}
