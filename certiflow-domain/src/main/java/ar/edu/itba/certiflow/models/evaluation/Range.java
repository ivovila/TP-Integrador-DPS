package ar.edu.itba.certiflow.models.evaluation;

import java.math.BigDecimal;

public record Range(BigDecimal minimum, BigDecimal maximum) {

    public Range {
        if (minimum.compareTo(maximum) > 0) {
            throw new InvalidRuleConfigurationException("Range minimum " + minimum + " exceeds maximum " + maximum);
        }
    }

    public boolean contains(BigDecimal value) {
        return minimum.compareTo(value) <= 0 && value.compareTo(maximum) <= 0;
    }
}
