package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SectionsJsonTest {

    @Test
    void sectionsSurviveTheRoundTripIncludingDecimalScale() {
        var sections = JpaInspectionSchemaRepositoryTest.sections();

        var read = SectionsJson.read(SectionsJson.write(sections));

        assertEquals(sections, read);
        assertEquals("6.00", ((ar.edu.itba.certiflow.schema.model.NumericRangeRule) read.getFirst().criteria().getFirst().rule())
                .range().minimum().toPlainString());
    }

    @Test
    void aSingleCriterionSurvivesTheRoundTrip() {
        var criterion = JpaInspectionSchemaRepositoryTest.sections().getFirst().criteria().getFirst();

        assertEquals(criterion, SectionsJson.readCriterion(SectionsJson.writeCriterion(criterion)));
    }
}
