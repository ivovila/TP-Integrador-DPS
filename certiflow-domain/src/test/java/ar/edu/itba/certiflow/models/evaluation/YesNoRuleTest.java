package ar.edu.itba.certiflow.models.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class YesNoRuleTest {

    @ParameterizedTest(name = "expecting {0}, answering {1} satisfies the rule: {2}")
    @CsvSource({
            "YES, YES, true",
            "YES, NO, false",
            "NO, NO, true",
            "NO, YES, false"
    })
    void onlyTheExpectedAnswerSatisfiesTheRule(YesNoAnswer expected, YesNoAnswer given, boolean satisfied) {
        YesNoRule rule = new YesNoRule(expected);

        assertEquals(satisfied, rule.isSatisfiedBy(given));
    }
}
