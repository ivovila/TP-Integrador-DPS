package ar.edu.itba.certiflow.harness.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.assistance.model.CriterionUnderReview;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Outcome;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** El control de formato del conjunto dorado (README de golden): un caso mal armado no mide lo que dice medir. */
class GoldenSetTest {

    private static final List<GoldenCase> CASES = new GoldenSet().cases();

    private final CaseInspections caseInspections = new CaseInspections(
            Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC));
    private final DemoAssets demoAssets = new DemoAssets();

    static List<GoldenCase> cases() {
        return CASES;
    }

    @Test
    void thereAreFifteenCasesNumberedInOrderWithTheMixTheSpecAsksFor() {
        assertEquals(IntStream.rangeClosed(1, 15).mapToObj(number -> String.format("case-%02d", number)).toList(),
                CASES.stream().map(GoldenCase::id).toList());
        assertEquals(Map.of("hidden", 10L, "clean", 3L, "trap", 2L),
                CASES.stream().collect(Collectors.groupingBy(GoldenCase::kind, Collectors.counting())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void onlyHiddenCasesExpectFindingsAndTheirSeverityIsTheConfiguredOne(GoldenCase goldenCase) {
        assertEquals(goldenCase.kind().equals("hidden"), !goldenCase.expected().isEmpty());
        List<Criterion<?>> criteria = demoAssets.of(goldenCase.assetType()).schema().version(goldenCase.schemaVersion())
                .criteria();
        goldenCase.expected().forEach(expected -> assertEquals(expected.severity(), criteria.stream()
                .filter(criterion -> criterion.code().equals(new CriterionCode(expected.criterion())))
                .findFirst().orElseThrow().severity().code(), goldenCase.id() + " " + expected.criterion()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void everyCaseAnswersEveryCriterionOfItsSchemaAndTheRuleApprovesItExceptTheTrapOnHose(GoldenCase goldenCase) {
        InspectionUnderReview inspection = caseInspections.underReview(goldenCase);
        Set<CriterionCode> schemaCriteria = demoAssets.of(goldenCase.assetType()).schema()
                .version(goldenCase.schemaVersion()).criteria().stream().map(Criterion::code).collect(Collectors.toSet());

        assertFalse(inspection.closed());
        assertEquals(schemaCriteria, inspection.criteria().stream().map(CriterionUnderReview::code).collect(Collectors.toSet()));
        assertTrue(inspection.criteria().stream().noneMatch(criterion -> criterion.answer().equals("not answered")));
        assertEquals(goldenCase.id().equals("case-15") ? Set.of(new CriterionCode("HOSE")) : Set.of(),
                inspection.criteria().stream().filter(criterion -> criterion.outcome() != Outcome.APPROVED)
                        .map(CriterionUnderReview::code).collect(Collectors.toSet()));
    }

    @Test
    void theAssistantReadsTheRuleTheAnswerAndTheFreeTextOfEachCriterion() {
        InspectionUnderReview inspection = caseInspections.underReview(CASES.get(9));
        CriterionUnderReview reliefValve = inspection.criteria().stream()
                .filter(criterion -> criterion.code().equals(new CriterionCode("RELIEF-VALVE"))).findFirst().orElseThrow();

        assertEquals("numeric-range (unit: bar; minimum: 7.5; maximum: 8.5)", reliefValve.rule());
        assertEquals("8.0", reliefValve.answer());
        assertEquals(List.of("Informe de la prueba en banco: la válvula recién abrió a 9,2 bar"),
                reliefValve.evidence().stream().map(evidence -> evidence.description()).toList());
    }
}
