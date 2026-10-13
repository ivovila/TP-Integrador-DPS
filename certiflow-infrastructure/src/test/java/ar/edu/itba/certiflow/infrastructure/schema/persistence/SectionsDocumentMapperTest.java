package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import org.junit.jupiter.api.Test;

class SectionsDocumentMapperTest {

    private final SectionsDocumentMapper sectionsDocuments = new SectionsDocumentMapper(new StoredJson());

    @Test
    void sectionsSurviveTheRoundTripIncludingDecimalScale() {
        var sections = DomainSamples.sections();

        var read = sectionsDocuments.read(sectionsDocuments.write(sections));

        assertEquals(sections, read);
        assertEquals("6.00", ((NumericRangeRule) read.getFirst().criteria().getFirst().rule())
                .range().minimum().toPlainString());
    }

    @Test
    void aSingleCriterionSurvivesTheRoundTrip() {
        var criterion = DomainSamples.sections().getFirst().criteria().getFirst();

        assertEquals(criterion, sectionsDocuments.readCriterion(sectionsDocuments.writeCriterion(criterion)));
    }
}
