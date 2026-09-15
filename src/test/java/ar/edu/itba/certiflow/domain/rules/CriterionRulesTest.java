package ar.edu.itba.certiflow.domain.rules;

import static ar.edu.itba.certiflow.support.Fixtures.pressure;
import static ar.edu.itba.certiflow.support.Fixtures.pressureResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ar.edu.itba.certiflow.domain.model.shared.EvidenceType;
import ar.edu.itba.certiflow.domain.model.shared.Measurement;
import ar.edu.itba.certiflow.domain.model.shared.Response;
import ar.edu.itba.certiflow.domain.model.shared.SelectedOption;
import ar.edu.itba.certiflow.domain.model.shared.YesNo;
import ar.edu.itba.certiflow.support.Fixtures;

class CriterionRulesTest {

    private final NumericRangeRule pressureRange =
            new NumericRangeRule("presion", "bar", new BigDecimal("10"), new BigDecimal("12"), BigDecimal.ONE);

    @Test
    void measurementInsideRangeIsApproved() {
        assertEquals(CriterionOutcome.APPROVED, pressureRange.evaluate(pressureResponse("11")));
    }

    @Test
    void measurementInsideToleranceIsObserved() {
        assertEquals(CriterionOutcome.OBSERVED, pressureRange.evaluate(pressureResponse("12.5")));
    }

    @Test
    void measurementOutsideToleranceIsRejected() {
        assertEquals(CriterionOutcome.REJECTED, pressureRange.evaluate(pressureResponse("14")));
    }

    @Test
    void missingMeasurementIsRejected() {
        assertEquals(CriterionOutcome.REJECTED, pressureRange.evaluate(Response.empty()));
    }

    @Test
    void rangeOnlyAcceptsItsMagnitudeAndUnit() {
        assertTrue(pressureRange.accepts(pressure("11")));
        assertFalse(pressureRange.accepts(new Measurement("presion", BigDecimal.TEN, "psi")));
    }

    @Test
    void missingRequiredEvidenceRejectsEvenWithPositiveAnswer() {
        CriterionRule signage = CompositeRule.allOf(new BooleanRule(true),
                new RequiredEvidenceRule(Set.of(EvidenceType.PHOTO)));

        assertEquals(CriterionOutcome.REJECTED, signage.evaluate(Response.empty().with(new YesNo(true))));
        assertEquals(CriterionOutcome.APPROVED, signage.evaluate(Fixtures.signageWithPhoto()));
    }

    @Test
    void compositeRuleKeepsTheWorstOutcome() {
        CompositeRule rule = CompositeRule.allOf(pressureRange,
                new EnumOptionRule(Set.of("rojo"), Set.of("desteñido")));
        Response response = pressureResponse("11").with(new SelectedOption("desteñido"));

        assertEquals(CriterionOutcome.OBSERVED, rule.evaluate(response));
        assertTrue(rule.accepts(pressure("11")));
    }

    @Test
    void compositeWithoutMeasurementRulesAcceptsNoMeasurement() {
        CompositeRule signage = CompositeRule.allOf(new BooleanRule(true),
                new RequiredEvidenceRule(Set.of(EvidenceType.PHOTO)));

        assertFalse(signage.accepts(pressure("11")));
    }

    @Test
    void criterionWithTwoMeasurementsEvaluatesBoth() {
        CompositeRule rule = CompositeRule.allOf(pressureRange,
                new NumericRangeRule("temperatura", "C", BigDecimal.ZERO, new BigDecimal("40"), BigDecimal.ZERO));
        Response bothInRange = pressureResponse("11").with(new Measurement("temperatura", new BigDecimal("35"), "C"));
        Response hot = pressureResponse("11").with(new Measurement("temperatura", new BigDecimal("55"), "C"));

        assertEquals(CriterionOutcome.APPROVED, rule.evaluate(bothInRange));
        assertEquals(CriterionOutcome.REJECTED, rule.evaluate(hot));
        assertEquals(CriterionOutcome.REJECTED, rule.evaluate(pressureResponse("11")));
    }

    @Test
    void unknownOptionIsRejected() {
        EnumOptionRule color = new EnumOptionRule(Set.of("rojo"), Set.of("desteñido"));

        assertEquals(CriterionOutcome.REJECTED, color.evaluate(Response.empty().with(new SelectedOption("verde"))));
    }
}
