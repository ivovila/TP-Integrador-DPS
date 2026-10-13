package ar.edu.itba.certiflow.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CriterionTest {

    private enum AdvisorySeverity implements Severity {
        ADVISORY;

        @Override
        public String code() {
            return name();
        }

        @Override
        public String label() {
            return "Advisory";
        }

        @Override
        public Outcome outcomeWhenUnmet() {
            return Outcome.OBSERVED;
        }

    }

    @ParameterizedTest(name = "unmet {0} criterion is {1}")
    @CsvSource({
            "MINOR, OBSERVED",
            "MAJOR, REJECTED",
            "CRITICAL, REJECTED"
    })
    void severityDecidesWhetherAnUnmetCriterionIsObservedOrRejected(StandardSeverity severity, Outcome expected) {
        Criterion<YesNoAnswer> criterion = criterionWith(severity);

        assertEquals(expected, criterion.outcomeOf(YesNoAnswer.NO));
    }

    @ParameterizedTest
    @CsvSource({"MINOR", "MAJOR", "CRITICAL"})
    void metCriterionIsApprovedWhateverItsSeverity(StandardSeverity severity) {
        Criterion<YesNoAnswer> criterion = criterionWith(severity);

        assertEquals(Outcome.APPROVED, criterion.outcomeOf(YesNoAnswer.YES));
    }

    @Test
    void customSeverityScaleIsHonouredWithoutChangingTheDomain() {
        Criterion<YesNoAnswer> criterion = criterionWith(AdvisorySeverity.ADVISORY);

        assertEquals(Outcome.OBSERVED, criterion.outcomeOf(YesNoAnswer.NO));
    }

    @Test
    void schemaVersionRejectsDuplicatedCriterionCodes() {
        Criterion<YesNoAnswer> firstCriterion = criterionWith(StandardSeverity.MINOR);
        Criterion<YesNoAnswer> criterionWithSameCode = new Criterion<>("C-1", "Another text", new YesNoRule(YesNoAnswer.NO),
                StandardSeverity.MAJOR, List.of());

        assertThrows(InvalidSchemaException.class,
                () -> new SchemaSections(List.of(new Section("General", List.of(firstCriterion, criterionWithSameCode)))));
    }

    @Test
    void criterionCannotRequireTheSameEvidenceKindTwice() {
        List<EvidenceRequirement> repeatedRequirements = List.of(
                new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1),
                new EvidenceRequirement(StandardEvidenceKind.PHOTO, 1));

        assertThrows(InvalidSchemaException.class, () -> new Criterion<>("C-1", "Guard is in place",
                new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, repeatedRequirements));
    }

    @Test
    void sectionWithoutCriteriaIsRejected() {
        assertThrows(InvalidSchemaException.class, () -> new Section("Empty", List.of()));
    }

    private Criterion<YesNoAnswer> criterionWith(Severity severity) {
        return new Criterion<>("C-1", "Guard is in place", new YesNoRule(YesNoAnswer.YES), severity, List.of());
    }

    @ParameterizedTest
    @CsvSource({"MINOR,OBSERVED,false", "MAJOR,REJECTED,true", "CRITICAL,REJECTED,true"})
    void anUnmetCriterionTakesTheOutcomeAndBlockingOfItsSeverity(StandardSeverity severity, Outcome outcome, boolean blocks) {
        Criterion<YesNoAnswer> criterion = new Criterion<>("C-1", "Seal", new YesNoRule(YesNoAnswer.YES), severity, List.of());

        assertEquals(outcome, criterion.outcomeOf(YesNoAnswer.NO));
        assertEquals(Outcome.APPROVED, criterion.outcomeOf(YesNoAnswer.YES));
    }
}
