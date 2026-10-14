package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.UnreadableStoredDataException;
import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.ApprovalRuleKind;
import ar.edu.itba.certiflow.schema.model.ApprovalRuleKindCatalog;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.NumericRangeRuleKind;
import ar.edu.itba.certiflow.schema.model.OptionAnswer;
import ar.edu.itba.certiflow.schema.model.OptionInListRuleKind;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.schema.model.RuleDescriber;
import ar.edu.itba.certiflow.schema.model.RuleParameters;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.YesNoRuleKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class SectionsDocumentMapperTest {

    /** Un tipo de regla que el código de producción no conoce. */
    record PrefixRule(String prefix) implements ApprovalRule<OptionAnswer> {

        @Override
        public boolean isSatisfiedBy(OptionAnswer answer) {
            return answer.option().startsWith(prefix);
        }

        @Override
        public OptionAnswer answerFrom(RawAnswer raw) {
            return new OptionAnswer(raw.text());
        }

        @Override
        public <T> T describedBy(RuleDescriber<T> describer) {
            return describer.text("prefix", prefix).ofKind("prefix");
        }
    }

    static final class PrefixRuleKind implements ApprovalRuleKind {

        @Override
        public boolean isNamed(String ruleKindName) {
            return "prefix".equals(ruleKindName);
        }

        @Override
        public ApprovalRule<?> ruleFrom(RuleParameters parameters) {
            return new PrefixRule(parameters.text("prefix"));
        }
    }

    private final SectionsDocumentMapper sectionsDocuments = mapperKnowing(new NumericRangeRuleKind(),
            new YesNoRuleKind(), new OptionInListRuleKind());

    private SectionsDocumentMapper mapperKnowing(ApprovalRuleKind... ruleKinds) {
        return new SectionsDocumentMapper(new StoredJson(), new SeverityCatalog(List.of(StandardSeverity.values())),
                new EvidenceKindCatalog(List.of(StandardEvidenceKind.values())),
                new ApprovalRuleKindCatalog(List.of(ruleKinds)));
    }

    @Test
    void sectionsSurviveTheRoundTripIncludingDecimalScale() {
        List<Section> sections = DomainSamples.sections();

        List<Section> read = sectionsDocuments.read(sectionsDocuments.write(sections));

        assertEquals(sections, read);
        assertEquals("6.00", ((NumericRangeRule) read.getFirst().criteria().getFirst().rule())
                .range().minimum().toPlainString());
    }

    @Test
    void eachRuleIsStoredWithItsKindAndOnlyItsOwnParameters() {
        String storedSections = sectionsDocuments.write(DomainSamples.sections());

        assertTrue(storedSections.contains("""
                "rule":{"type":"numeric-range","unit":"bar","minimum":6.00,"maximum":8.00}"""), storedSections);
        assertTrue(storedSections.contains("""
                "rule":{"type":"yes-no","expected":"YES"}"""), storedSections);
        assertTrue(storedSections.contains("""
                "rule":{"type":"option-in-list","options":["REFLECTIVE","VISIBLE"]}"""), storedSections);
    }

    /** Así se guardaban las reglas antes: con vacíos los parámetros de los otros tipos. Esas filas siguen ahí. */
    @Test
    void aRuleStoredWithTheEmptyParametersOfTheOtherKindsIsStillRead() {
        Criterion<?> criterion = DomainSamples.sections().getFirst().criteria().getFirst();
        String storedBefore = sectionsDocuments.writeCriterion(criterion).replace(
                "\"maximum\":8.00}", "\"maximum\":8.00,\"expected\":null,\"options\":null}");

        assertTrue(storedBefore.contains("\"options\":null"), storedBefore);
        assertEquals(criterion, sectionsDocuments.readCriterion(storedBefore));
    }

    @Test
    void aNewKindOfRuleIsStoredAndReadByRegisteringItWithoutChangingTheMapper() {
        SectionsDocumentMapper mapperThatKnowsPrefixRules = mapperKnowing(new NumericRangeRuleKind(), new PrefixRuleKind());
        Criterion<OptionAnswer> criterion = new Criterion<>(new CriterionCode("TAG-01"), "Tag starts with the plant code",
                new PrefixRule("PLANT-"), StandardSeverity.MINOR, List.of());

        String storedCriterion = mapperThatKnowsPrefixRules.writeCriterion(criterion);

        assertTrue(storedCriterion.contains("""
                "rule":{"type":"prefix","prefix":"PLANT-"}"""), storedCriterion);
        assertEquals(criterion, mapperThatKnowsPrefixRules.readCriterion(storedCriterion));
    }

    @Test
    void aStoredRuleOfAnUnknownTypeIsReportedAsUnreadable() {
        String storedCriterion = sectionsDocuments.writeCriterion(DomainSamples.sections().getFirst().criteria().getFirst())
                .replace("numeric-range", "newer-rule");

        assertThrows(UnreadableStoredDataException.class, () -> sectionsDocuments.readCriterion(storedCriterion));
    }

    @Test
    void aStoredRuleThatLacksOneOfItsParametersIsReportedAsUnreadable() {
        String storedCriterion = sectionsDocuments.writeCriterion(DomainSamples.sections().getFirst().criteria().getFirst())
                .replace("\"unit\":\"bar\",", "");

        assertThrows(UnreadableStoredDataException.class, () -> sectionsDocuments.readCriterion(storedCriterion));
    }

    @Test
    void aSingleCriterionSurvivesTheRoundTrip() {
        Criterion<?> criterion = DomainSamples.sections().getFirst().criteria().getFirst();

        assertEquals(criterion, sectionsDocuments.readCriterion(sectionsDocuments.writeCriterion(criterion)));
    }
}
