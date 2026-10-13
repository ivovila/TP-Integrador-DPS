package ar.edu.itba.certiflow.certification.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RequirementsTest {

    private final Requirements<Integer> positiveAndEven = new Requirements<>(List.of(
            new Requirement<>(number -> number > 0, StandardIneligibilityReason.INSPECTION_NOT_CLOSED,
                    number -> number + " is not positive"),
            new Requirement<>(number -> number % 2 == 0, StandardIneligibilityReason.PARTIAL_INSPECTION,
                    number -> number + " is not even")));

    @Test
    void aCandidateThatMeetsEveryRequirementHasNoIneligibilities() {
        assertEquals(List.of(), positiveAndEven.unmetBy(4));
        assertTrue(positiveAndEven.areMetBy(4));
    }

    @Test
    void eachUnmetRequirementExplainsItselfInTheOrderItWasDeclared() {
        assertEquals(List.of(
                        new Ineligibility(StandardIneligibilityReason.INSPECTION_NOT_CLOSED, "-3 is not positive"),
                        new Ineligibility(StandardIneligibilityReason.PARTIAL_INSPECTION, "-3 is not even")),
                positiveAndEven.unmetBy(-3));
        assertFalse(positiveAndEven.areMetBy(-3));
    }
}
