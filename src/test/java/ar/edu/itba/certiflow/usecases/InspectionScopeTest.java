package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.models.evaluation.OptionAnswer;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.SectionsScope;
import ar.edu.itba.certiflow.models.inspection.exceptions.CriterionOutOfScopeException;
import ar.edu.itba.certiflow.models.inspection.exceptions.PartialInspectionCannotCertifyException;
import ar.edu.itba.certiflow.models.schema.exceptions.SectionNotInSchemaException;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InspectionScopeTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void partialInspectionOnlyTakesAnswersForItsSectionsAndClosesWhenThoseAreComplete() {
        Inspection inspection = fixture.assignedInspection(new SectionsScope(Set.of(CertiflowFixture.SIGNAGE)));

        assertThrows(CriterionOutOfScopeException.class, () -> fixture.recordAnswer.execute(inspection,
                fixture.pressureCriterion, fixture.numericAnswer("7.00"), fixture.inspector));
        fixture.recordAnswer.execute(inspection, fixture.signageCriterion, new OptionAnswer("VISIBLE"), fixture.inspector);
        fixture.closeInspection.execute(inspection, fixture.inspector);

        assertTrue(inspection.isClosed());
        assertFalse(inspection.coversWholeSchema());
        assertEquals(1, inspection.evaluate().responses().size());
    }

    @Test
    void scopeNamingASectionOutsideTheSchemaIsRejectedWhenAssigning() {
        assertThrows(SectionNotInSchemaException.class,
                () -> fixture.assignedInspection(new SectionsScope(Set.of("Hose condition"))));
    }

    @Test
    void scopeCoveringEverySectionCountsAsAWholeInspection() {
        Inspection inspection = fixture.assignedInspection(
                new SectionsScope(Set.of(CertiflowFixture.PRESSURE_AND_SEAL, CertiflowFixture.SIGNAGE)));

        assertTrue(inspection.coversWholeSchema());
    }

    @Test
    void partialInspectionCannotBackACertificate() {
        Inspection inspection = fixture.assignedInspection(new SectionsScope(Set.of(CertiflowFixture.SIGNAGE)));
        fixture.recordAnswer.execute(inspection, fixture.signageCriterion, new OptionAnswer("VISIBLE"), fixture.inspector);
        fixture.closeInspection.execute(inspection, fixture.inspector);

        assertThrows(PartialInspectionCannotCertifyException.class, () -> fixture.issueCertificate.execute(inspection,
                CertiflowFixture.TODAY.plusYears(1), fixture.certifier));
    }
}
