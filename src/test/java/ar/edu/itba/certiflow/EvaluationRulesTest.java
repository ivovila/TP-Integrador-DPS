package ar.edu.itba.certiflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.domain.Answer;
import ar.edu.itba.certiflow.domain.Criterion;
import ar.edu.itba.certiflow.domain.DomainException;
import ar.edu.itba.certiflow.domain.Evidence;
import ar.edu.itba.certiflow.domain.Submission;
import ar.edu.itba.certiflow.details.rules.BooleanRule;
import ar.edu.itba.certiflow.details.rules.DocumentaryRule;
import ar.edu.itba.certiflow.details.rules.NumericRangeRule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@org.junit.jupiter.api.Tag("unit")
class EvaluationRulesTest {
    @ParameterizedTest
    @CsvSource({
        "-0.01,REJECTED",
        "0,OBSERVED",
        "9.99,OBSERVED",
        "10,APPROVED",
        "30,APPROVED",
        "30.01,OBSERVED",
        "50,OBSERVED",
        "50.01,REJECTED"
    })
    void numericBoundariesAreInclusive(String value, Outcome expected) {
        var rule =
                new NumericRangeRule(
                        new BigDecimal("0"),
                        new BigDecimal("10"),
                        new BigDecimal("30"),
                        new BigDecimal("50"),
                        "C");
        assertEquals(
                expected, rule.evaluate(new Answer.Numeric(new BigDecimal(value), "C")).outcome());
    }

    @Test
    void wrongTypeAndUnitAreRejectedInsteadOfSilentlyConverted() {
        var f = new Fixture();
        var c = f.schemeRepo.versions(f.schemeId).get(0).criterion("TEMP");
        assertThrows(
                DomainException.class,
                () -> c.evaluate(new Submission(new Answer.YesNo(true), List.of(), "")));
        assertThrows(
                DomainException.class,
                () ->
                        c.evaluate(
                                new Submission(
                                        new Answer.Numeric(BigDecimal.TEN, "F"), List.of(), "")));
    }

    @Test
    void missingAnswerOrRequiredEvidenceIsIncomplete() {
        var f = new Fixture();
        var version = f.schemeRepo.versions(f.schemeId).get(0);
        assertEquals(Outcome.INCOMPLETE, version.criterion("TEMP").evaluate(null).outcome());
        assertEquals(
                Outcome.INCOMPLETE,
                version.criterion("TEMP")
                        .evaluate(
                                new Submission(
                                        new Answer.Numeric(BigDecimal.TEN, "C"), List.of(), ""))
                        .outcome());
        assertEquals(
                Outcome.INCOMPLETE,
                version.criterion("DOC")
                        .evaluate(
                                new Submission(
                                        new Answer.Documentary(),
                                        List.of(f.evidence(Evidence.Kind.PHOTO)),
                                        ""))
                        .outcome());
    }

    @Test
    void booleanMismatchCanBeObservedOrRejected() {
        assertEquals(
                Outcome.OBSERVED,
                new BooleanRule(true, Outcome.OBSERVED)
                        .evaluate(new Answer.YesNo(false))
                        .outcome());
        assertEquals(
                Outcome.REJECTED,
                new BooleanRule(true, Outcome.REJECTED)
                        .evaluate(new Answer.YesNo(false))
                        .outcome());
        assertEquals(
                Outcome.APPROVED,
                new BooleanRule(false, Outcome.REJECTED)
                        .evaluate(new Answer.YesNo(false))
                        .outcome());
    }

    @Test
    void invalidRuleDefinitionsCannotBePublished() {
        assertThrows(
                DomainException.class,
                () ->
                        new NumericRangeRule(
                                BigDecimal.TEN,
                                BigDecimal.ZERO,
                                BigDecimal.TEN,
                                BigDecimal.TEN,
                                "C"));
        assertThrows(DomainException.class, () -> new BooleanRule(true, Outcome.APPROVED));
        assertThrows(
                DomainException.class,
                () ->
                        new Criterion(
                                "D", "Document", Severity.MINOR, Set.of(), new DocumentaryRule()));
    }

    @Test
    void duplicateCriterionCodesAcrossSectionsAreRejected() {
        var f = new Fixture();
        var section = f.schemeRepo.versions(f.schemeId).get(0).sections().get(0);
        assertThrows(
                DomainException.class,
                () ->
                        f.catalog.publish(
                                f.schemeId,
                                "Bad",
                                new ar.edu.itba.certiflow.domain.AssetType("LAB", "Laboratorio"),
                                List.of(section, section),
                                365,
                                "author"));
        assertEquals(1, f.schemeRepo.versions(f.schemeId).size());
    }
}
