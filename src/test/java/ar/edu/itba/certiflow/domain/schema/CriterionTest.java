package ar.edu.itba.certiflow.domain.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.domain.evaluation.Outcome;
import ar.edu.itba.certiflow.domain.evaluation.Severity;
import ar.edu.itba.certiflow.domain.evaluation.StandardSeverity;
import ar.edu.itba.certiflow.domain.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.details.evaluation.YesNoRule;
import java.time.Instant;
import java.util.List;

import ar.edu.itba.certiflow.domain.schema.exceptions.InvalidSchemaException;
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
        Criterion<YesNoAnswer> first = criterionWith(StandardSeverity.MINOR);
        Criterion<YesNoAnswer> sameCode = new Criterion<>("C-1", "Another text", new YesNoRule(YesNoAnswer.NO),
                StandardSeverity.MAJOR, List.of());

        assertThrows(InvalidSchemaException.class, () -> new SchemaVersion(1,
                List.of(new Section("General", List.of(first, sameCode))), Instant.EPOCH));
    }

    @Test
    void sectionWithoutCriteriaIsRejected() {
        assertThrows(InvalidSchemaException.class, () -> new Section("Empty", List.of()));
    }

    private Criterion<YesNoAnswer> criterionWith(Severity severity) {
        return new Criterion<>("C-1", "Guard is in place", new YesNoRule(YesNoAnswer.YES), severity, List.of());
    }
}
