package ar.edu.itba.certiflow.domain.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import ar.edu.itba.certiflow.details.evaluation.OptionAnswer;
import ar.edu.itba.certiflow.details.evaluation.OptionInListRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OptionInListRuleTest {

    private final OptionInListRule rule = new OptionInListRule(Set.of("VISIBLE", "REFLECTIVE"));

    @ParameterizedTest(name = "option {0} satisfies the rule: {1}")
    @CsvSource({
            "VISIBLE, true",
            "REFLECTIVE, true",
            "FADED, false",
            "visible, false"
    })
    void onlyListedOptionsSatisfyTheRule(String option, boolean satisfied) {
        assertEquals(satisfied, rule.isSatisfiedBy(new OptionAnswer(option)));
    }

    @Test
    void ruleWithoutAcceptedOptionsIsAnInvalidConfiguration() {
        assertThrows(InvalidRuleConfigurationException.class, () -> new OptionInListRule(Set.of()));
    }
}
