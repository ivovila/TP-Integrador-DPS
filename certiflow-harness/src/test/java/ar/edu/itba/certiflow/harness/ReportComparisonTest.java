package ar.edu.itba.certiflow.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Cuándo una versión de prompt empeora a la anterior, y cómo quedan los archivos que se versionan. */
class ReportComparisonTest {

    private HarnessReport report(String promptVersion, Statistic correct, Statistic invented) {
        return new HarnessReport(promptVersion, "nvidia/nemotron", 5, Instant.parse("2026-11-08T12:00:00Z"),
                Map.of(Metric.CORRECT, correct, Metric.SEVERITY_HITS, new Statistic(5.0, 5, 5), Metric.INVENTED, invented,
                        Metric.MISSED, new Statistic(3.0, 2, 4)),
                Map.of(), List.of(new CaseResult("case", "positive", "fixture", List.of(),
                        java.util.stream.IntStream.rangeClosed(1, 5)
                                .mapToObj(index -> new RunResult(index, 1, 1, 0, 0, List.of(), List.of())).toList(),
                        List.of(), "same-input")));
    }

    private final HarnessReport v1 = report("v1", new Statistic(8.0, 7, 9), new Statistic(1.0, 0, 2));

    @Test
    void aDifferenceInsideTheObservedRangeIsNoiseNotARegression() {
        HarnessReport v2 = report("v2", new Statistic(7.2, 6, 8), new Statistic(1.8, 1, 3));

        assertEquals(List.of(), new ReportComparison(v1, v2).regressions());
    }

    @Test
    void aMeanOutsideTheObservedRangeInTheWrongDirectionIsARegression() {
        HarnessReport v2 = report("v2", new Statistic(6.8, 6, 8), new Statistic(2.2, 1, 3));
        ReportComparison comparison = new ReportComparison(v1, v2);

        assertEquals(List.of(Metric.CORRECT, Metric.INVENTED), comparison.regressions());
        assertTrue(comparison.markdown().contains("| Hallazgos correctos | 8.00 (7–9) | 6.80 (6–8) | **sí** |"));
        assertTrue(comparison.markdown().contains("Empeoraron: Hallazgos correctos, Hallazgos inventados."));
    }

    @Test
    void betterMetricsAreNeverARegression() {
        HarnessReport v2 = report("v2", new Statistic(10.0, 10, 10), new Statistic(0.0, 0, 0));

        assertEquals(List.of(), new ReportComparison(v1, v2).regressions());
    }

    @Test
    void theReportIsWrittenUnderItsPromptVersionAndReadBackUnchanged(@TempDir Path results) throws Exception {
        ReportFiles files = new ReportFiles(results);
        HarnessReport v2 = report("v2", new Statistic(9.0, 8, 10), new Statistic(0.4, 0, 1));

        Path written = files.write(v2);
        Path comparison = files.write(new ReportComparison(v1, v2));

        assertEquals(results.resolve("v2").resolve("nvidia_nemotron.json"), written);
        assertEquals(v2, files.read(written));
        assertTrue(Files.readString(results.resolve("v2").resolve("nvidia_nemotron.md")).startsWith("# Harness · prompt v2"));
        assertEquals(results.resolve("v2").resolve("comparison-with-v1.md"), comparison);
    }
    @Test
    void incompleteReportsCannotClaimThatTheCandidateImproved() {
        HarnessReport incomplete = new HarnessReport("v2", v1.model(), 5, v1.ranAt(), List.of(
                new CaseResult("case", "positive", "fixture", List.of(), List.of(), List.of("timeout"), "same-input")));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ReportComparison(v1, incomplete));
    }

    @Test
    void comparisonRequiresTheSameModelAndInput() {
        HarnessReport differentModel = new HarnessReport("v2", "other-model", 5, v1.ranAt(), v1.cases());
        CaseResult changed = new CaseResult("case", "positive", "fixture", List.of(),
                v1.cases().getFirst().runs(), List.of(), "changed-input");
        HarnessReport differentInput = new HarnessReport("v2", v1.model(), 5, v1.ranAt(), List.of(changed));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ReportComparison(v1, differentModel));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ReportComparison(v1, differentInput));
    }

    @Test
    void totalsExcludeAnEntireIncompleteRepetitionInsteadOfTreatingFailuresAsZero() {
        RunResult measured = new RunResult(1, 1, 1, 2, 0, List.of(), List.of());
        CaseResult first = new CaseResult("a", "positive", "a", List.of(), List.of(measured,
                new RunResult(2, 1, 1, 0, 0, List.of(), List.of())), List.of(), "a");
        CaseResult second = new CaseResult("b", "positive", "b", List.of(), List.of(measured), List.of("timeout"), "b");
        HarnessReport report = new HarnessReport("v1", "model", 2, v1.ranAt(), List.of(first, second));
        assertEquals(1, report.completeRuns());
        assertEquals(new Statistic(4.0, 4, 4), report.totals().get(Metric.INVENTED));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ReportComparison(report, report));
    }

}
