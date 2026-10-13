package ar.edu.itba.certiflow.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.certification.model.StandardIneligibilityReason;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.SectionsScope;
import ar.edu.itba.certiflow.inspection.model.exceptions.CriterionOutOfScopeException;
import ar.edu.itba.certiflow.scenario.support.CertiflowFixture;
import ar.edu.itba.certiflow.scenario.support.ScenarioTest;
import ar.edu.itba.certiflow.schema.model.OptionAnswer;
import ar.edu.itba.certiflow.schema.model.SectionTitle;
import ar.edu.itba.certiflow.schema.model.exceptions.SectionNotInSchemaException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InspectionScopeTest extends ScenarioTest {

    @Test
    void partialInspectionOnlyTakesAnswersForItsSectionsAndClosesWhenThoseAreComplete() {
        InspectionId inspection = fixture.assignedInspection(new SectionsScope(Set.of(CertiflowFixture.SIGNAGE)));

        assertThrows(CriterionOutOfScopeException.class, () -> fixture.answer(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), fixture.inspector));
        fixture.answer(inspection, fixture.signageCriterion, new OptionAnswer("VISIBLE"), fixture.inspector);
        fixture.close(inspection, fixture.inspector);

        assertTrue(fixture.inspection(inspection).isClosed());
        assertFalse(fixture.inspection(inspection).coversWholeSchema());
        assertEquals(1, fixture.inspection(inspection).evaluate().responses().size());
    }

    @Test
    void scopeNamingASectionOutsideTheSchemaIsRejectedWhenAssigning() {
        assertThrows(SectionNotInSchemaException.class,
                () -> fixture.assignedInspection(new SectionsScope(Set.of(new SectionTitle("Hose condition")))));
    }

    @Test
    void scopeCoveringEverySectionCountsAsAWholeInspection() {
        InspectionId inspection = fixture.assignedInspection(
                new SectionsScope(Set.of(CertiflowFixture.PRESSURE_AND_SEAL, CertiflowFixture.SIGNAGE)));

        assertTrue(fixture.inspection(inspection).coversWholeSchema());
    }

    @Test
    void partialInspectionCannotBackACertificate() {
        InspectionId inspection = fixture.assignedInspection(new SectionsScope(Set.of(CertiflowFixture.SIGNAGE)));
        fixture.answer(inspection, fixture.signageCriterion, new OptionAnswer("VISIBLE"), fixture.inspector);
        fixture.close(inspection, fixture.inspector);

        CertiflowFixture.assertNotEligible(StandardIneligibilityReason.PARTIAL_INSPECTION, () -> fixture.issue(inspection, fixture.certifier));
    }
}
