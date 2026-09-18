package ar.edu.itba.certiflow.domain.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NumericRangeRuleTest {

    private static final Unit BAR = new Unit("bar");

    private final NumericRangeRule rule = new NumericRangeRule(BAR, new Range(new BigDecimal("6.00"), new BigDecimal("8.00")));

    @ParameterizedTest(name = "{0} bar satisfies the rule: {1}")
    @CsvSource({
            "6.00, true",
            "8.00, true",
            "7.25, true",
            "5.99, false",
            "8.01, false"
    })
    void rangeLimitsAreInclusiveAndAnythingBeyondThemFails(String value, boolean expected) {
        NumericAnswer answer = new NumericAnswer(new BigDecimal(value));

        boolean satisfied = rule.isSatisfiedBy(answer);

        assertEquals(expected, satisfied);
    }

    @Test
    void sameValueWithDifferentScaleIsStillInsideTheRange() {
        NumericAnswer answer = new NumericAnswer(new BigDecimal("8"));

        assertEquals(true, rule.isSatisfiedBy(answer));
    }

    @Test
    void rangeWhoseMinimumExceedsItsMaximumIsAnInvalidConfiguration() {
        assertThrows(InvalidRuleConfigurationException.class,
                () -> new Range(new BigDecimal("8.00"), new BigDecimal("6.00")));
    }

    @Test
    void unitIsPartOfTheRuleConfigurationRatherThanTheAnswer() {
        assertEquals(BAR, rule.unit());
    }

    @Test
    void rangeWithEqualLimitsAcceptsExactlyThatValue() {
        Range exact = new Range(new BigDecimal("5"), new BigDecimal("5"));

        assertEquals(true, exact.contains(new BigDecimal("5.0")));
        assertEquals(false, exact.contains(new BigDecimal("5.1")));
    }
}
