package ar.edu.itba.certiflow.models.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.models.evaluation.EvidenceRequirement;
import ar.edu.itba.certiflow.models.evaluation.Outcome;
import ar.edu.itba.certiflow.models.evaluation.Severity;
import ar.edu.itba.certiflow.models.evaluation.StandardEvidenceKind;
import ar.edu.itba.certiflow.models.evaluation.StandardSeverity;
import ar.edu.itba.certiflow.models.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.models.evaluation.YesNoRule;
import ar.edu.itba.certiflow.models.schema.exceptions.InvalidSchemaException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CriterionTest {

    private enum AdvisorySeverity implements Severity {
        ADVISORY;

        @Override
        public String label() {
            return "Advisory";
        }

        @Override
        public Outcome outcomeWhenUnmet() {
            return Outcome.OBSERVED;
        }

        @Override
        public boolean blocksCertification() {
            return false;
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

        assertThrows(InvalidSchemaException.class, () -> new SchemaVersion(1,
                List.of(new Section("General", List.of(firstCriterion, criterionWithSameCode))), Instant.EPOCH));
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
}
