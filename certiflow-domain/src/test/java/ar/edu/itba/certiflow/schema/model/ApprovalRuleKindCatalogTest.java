package ar.edu.itba.certiflow.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.schema.model.exceptions.InvalidRuleConfigurationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApprovalRuleKindCatalogTest {

    /** Anota lo que la regla dice de sí misma, en el orden en que lo dice. */
    static final class WrittenRule implements RuleDescriber<List<String>> {

        private final List<String> written = new ArrayList<>();

        @Override
        public RuleDescriber<List<String>> text(String parameterName, String value) {
            written.add(parameterName + "=" + value);
            return this;
        }

        @Override
        public RuleDescriber<List<String>> decimal(String parameterName, BigDecimal value) {
            written.add(parameterName + "=" + value.toPlainString());
            return this;
        }

        @Override
        public RuleDescriber<List<String>> texts(String parameterName, Set<String> values) {
            written.add(parameterName + "=" + values.stream().sorted().toList());
            return this;
        }

        @Override
        public List<String> ofKind(String ruleKindName) {
            written.addFirst(ruleKindName);
            return written;
        }
    }

    record GivenParameters(Map<String, String> texts, Map<String, BigDecimal> decimals, Map<String, Set<String>> lists)
            implements RuleParameters {

        @Override
        public String text(String parameterName) {
            return texts.get(parameterName);
        }

        @Override
        public BigDecimal decimal(String parameterName) {
            return decimals.get(parameterName);
        }

        @Override
        public Set<String> texts(String parameterName) {
            return lists.get(parameterName);
        }
    }

    private final ApprovalRuleKindCatalog catalog = new ApprovalRuleKindCatalog(
            List.of(new NumericRangeRuleKind(), new YesNoRuleKind(), new OptionInListRuleKind()));

    @Test
    void eachRuleSaysItsKindAndItsParameters() {
        ApprovalRule<?> pressure = new NumericRangeRule(new Unit("bar"), new Range(new BigDecimal("6.00"), new BigDecimal("8.00")));
        ApprovalRule<?> seal = new YesNoRule(YesNoAnswer.YES);
        ApprovalRule<?> signage = new OptionInListRule(Set.of("VISIBLE", "REFLECTIVE"));

        assertEquals(List.of("numeric-range", "unit=bar", "minimum=6.00", "maximum=8.00"), pressure.describedBy(new WrittenRule()));
        assertEquals(List.of("yes-no", "expected=YES"), seal.describedBy(new WrittenRule()));
        assertEquals(List.of("option-in-list", "options=[REFLECTIVE, VISIBLE]"), signage.describedBy(new WrittenRule()));
    }

    @Test
    void aRuleIsBuiltAgainFromTheKindAndTheParametersItGave() {
        RuleParameters parameters = new GivenParameters(Map.of("unit", "bar", "expected", "NO"),
                Map.of("minimum", new BigDecimal("6.00"), "maximum", new BigDecimal("8.00")),
                Map.of("options", Set.of("VISIBLE")));

        assertEquals(new NumericRangeRule(new Unit("bar"), new Range(new BigDecimal("6.00"), new BigDecimal("8.00"))),
                catalog.named("numeric-range").orElseThrow().ruleFrom(parameters));
        assertEquals(new YesNoRule(YesNoAnswer.NO), catalog.named("yes-no").orElseThrow().ruleFrom(parameters));
        assertEquals(new OptionInListRule(Set.of("VISIBLE")), catalog.named("option-in-list").orElseThrow().ruleFrom(parameters));
    }

    @Test
    void aKindThatNobodyRegisteredIsNotFound() {
        assertTrue(catalog.named("prefix").isEmpty());
    }

    @Test
    void aYesNoRuleExpectsOneOfItsTwoAnswers() {
        RuleParameters parameters = new GivenParameters(Map.of("expected", "MAYBE"), Map.of(), Map.of());

        assertThrows(InvalidRuleConfigurationException.class, () -> new YesNoRuleKind().ruleFrom(parameters));
    }
}
