package ar.edu.itba.certiflow.harness;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** El resumen legible de un reporte: lo que se mira en el pull request y en la presentación. */
public final class ReportMarkdown {

    public String render(HarnessReport report) {
        return "# Harness · prompt " + report.promptVersion() + " · " + report.model() + "\n\n"
                + report.cases().size() + " casos, " + report.runsPerCase() + " corridas por caso, " + report.ranAt()
                + ". Corridas sin respuesta del proveedor: " + report.failures() + ".\n\n"
                + "Repeticiones completas medidas: " + report.completeRuns() + " de " + report.runsPerCase()
                + ". Las fallas no se cuentan como ceros.\n\n"
                + "## Totales por corrida\n\n"
                + "| Métrica | Media | Rango |\n|---|---|---|\n"
                + Arrays.stream(Metric.values()).map(metric -> "| " + metric.label() + " | "
                        + Optional.ofNullable(report.totals().get(metric)).map(this::mean).orElse("—") + " | "
                        + Optional.ofNullable(report.totals().get(metric)).map(this::range).orElse("—") + " |")
                        .collect(Collectors.joining("\n"))
                + "\n\nPropuestas frenadas por guardrail (todas las corridas): "
                + (report.rejectedByGuardrail().isEmpty() ? "ninguna" : report.rejectedByGuardrail().toString()) + ".\n\n"
                + "## Por caso (media de las corridas)\n\n"
                + "| Caso | Tipo | Esperado | Correctos | Severidad | Inventados | Omitidos | Fallas |\n"
                + "|---|---|---|---|---|---|---|---|\n"
                + report.cases().stream().map(this::row).collect(Collectors.joining("\n")) + "\n";
    }

    private String row(CaseResult result) {
        return "| " + result.id() + " | " + result.kind() + " | "
                + (result.expected().isEmpty() ? "—" : String.join(", ", result.expected())) + " | "
                + Arrays.stream(Metric.values())
                        .map(metric -> Optional.ofNullable(result.statistics().get(metric)).map(this::mean).orElse("—"))
                        .collect(Collectors.joining(" | "))
                + " | " + result.failures().size() + " |";
    }

    private String mean(Statistic statistic) {
        return String.format(Locale.ROOT, "%.2f", statistic.mean());
    }

    private String range(Statistic statistic) {
        return statistic.min() + "–" + statistic.max();
    }
}
