package ar.edu.itba.certiflow.harness;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Una versión de prompt contra la anterior, con el mismo modelo y el mismo conjunto dorado. Una métrica empeora
 * cuando su media nueva queda fuera del rango observado antes ({@link Metric#worsened}).
 */
public record ReportComparison(HarnessReport baseline, HarnessReport candidate) {

    public ReportComparison {
        validate(baseline);
        validate(candidate);
        if (!baseline.model().equals(candidate.model()) || baseline.runsPerCase() != candidate.runsPerCase()) {
            throw new IllegalArgumentException("Comparison requires the same model and repetitions");
        }
        java.util.Map<String, String> baselineInputs = inputs(baseline);
        if (!baselineInputs.equals(inputs(candidate))) {
            throw new IllegalArgumentException("Comparison requires the same versioned evaluation inputs and expectations");
        }
    }

    private static java.util.Map<String, String> inputs(HarnessReport report) {
        return report.cases().stream().collect(Collectors.toMap(CaseResult::id, CaseResult::inputFingerprint));
    }

    private static void validate(HarnessReport report) {
        if (report.failures() != 0 || report.cases().isEmpty() || report.completeRuns() != report.runsPerCase()
                || report.cases().stream().anyMatch(result -> result.inputFingerprint() == null
                        || result.inputFingerprint().isBlank() || result.runs().size() != report.runsPerCase())) {
            throw new IllegalArgumentException("Cannot compare incomplete or legacy reports: rerun the versioned evaluation");
        }
    }

    public List<Metric> regressions() {
        return Arrays.stream(Metric.values())
                .filter(metric -> metric.worsened(baseline.totals().get(metric), candidate.totals().get(metric)))
                .toList();
    }

    public String markdown() {
        List<Metric> regressions = regressions();
        return "# Prompt " + candidate.promptVersion() + " contra " + baseline.promptVersion() + "\n\n"
                + "Modelo: " + candidate.model() + ". Casos: " + candidate.cases().size() + ", " + candidate.runsPerCase()
                + " corridas por caso.\n\n"
                + "| Métrica | " + baseline.promptVersion() + " (media, rango) | " + candidate.promptVersion()
                + " (media, rango) | ¿Empeoró? |\n|---|---|---|---|\n"
                + Arrays.stream(Metric.values()).map(metric -> "| " + metric.label() + " | "
                        + describe(baseline.totals().get(metric)) + " | " + describe(candidate.totals().get(metric)) + " | "
                        + (regressions.contains(metric) ? "**sí**" : "no") + " |")
                        .collect(Collectors.joining("\n"))
                + "\n\n" + (regressions.isEmpty() ? "Ninguna métrica empeoró." : "Empeoraron: "
                        + regressions.stream().map(Metric::label).collect(Collectors.joining(", ")) + ".")
                + "\n";
    }

    private String describe(Statistic statistic) {
        return String.format(Locale.ROOT, "%.2f (%d–%d)", statistic.mean(), statistic.min(), statistic.max());
    }
}
