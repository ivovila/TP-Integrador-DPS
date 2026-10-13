package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.UnreadableStoredDataException;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.util.List;
import org.junit.jupiter.api.Test;

class SectionsDocumentMapperTest {

    private final SectionsDocumentMapper sectionsDocuments = new SectionsDocumentMapper(new StoredJson(),
            new SeverityCatalog(List.of(StandardSeverity.values())),
            new EvidenceKindCatalog(List.of(StandardEvidenceKind.values())),
            List.of(new NumericRangeStoredRuleReader(), new YesNoStoredRuleReader(), new OptionInListStoredRuleReader()));

    @Test
    void sectionsSurviveTheRoundTripIncludingDecimalScale() {
        var sections = DomainSamples.sections();

        var read = sectionsDocuments.read(sectionsDocuments.write(sections));

        assertEquals(sections, read);
        assertEquals("6.00", ((NumericRangeRule) read.getFirst().criteria().getFirst().rule())
                .range().minimum().toPlainString());
    }

    @Test
    void aRuleKeepsTheStoredShapeWithEveryFieldOfTheOtherKindsEmpty() {
        String storedSections = sectionsDocuments.write(DomainSamples.sections());

        assertTrue(storedSections.contains("""
                "rule":{"type":"numeric-range","unit":"bar","minimum":6.00,"maximum":8.00,\
                "expected":null,"options":null}""".strip()), storedSections);
    }

    @Test
    void aStoredRuleOfAnUnknownTypeIsReportedAsUnreadable() {
        String storedCriterion = sectionsDocuments.writeCriterion(DomainSamples.sections().getFirst().criteria().getFirst())
                .replace("numeric-range", "newer-rule");

        assertThrows(UnreadableStoredDataException.class, () -> sectionsDocuments.readCriterion(storedCriterion));
    }

    @Test
    void aSingleCriterionSurvivesTheRoundTrip() {
        var criterion = DomainSamples.sections().getFirst().criteria().getFirst();

        assertEquals(criterion, sectionsDocuments.readCriterion(sectionsDocuments.writeCriterion(criterion)));
    }
}
