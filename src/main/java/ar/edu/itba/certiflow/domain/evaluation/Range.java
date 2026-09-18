package ar.edu.itba.certiflow.domain.evaluation;


import java.math.BigDecimal;
import java.util.Objects;

public record Range(BigDecimal min, BigDecimal max) {

    public Range {
        if (min.compareTo(max) > 0) {
            throw new InvalidRuleConfigurationException("Range minimum " + min + " exceeds maximum " + max);
        }
    }

    public boolean contains(BigDecimal value) {
        return min.compareTo(value) <= 0 && value.compareTo(max) <= 0;
    }
}
