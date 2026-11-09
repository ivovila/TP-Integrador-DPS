package ar.edu.itba.certiflow.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.assistance.model.AssistantReply;
import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.assistance.model.ModelId;
import ar.edu.itba.certiflow.assistance.model.PromptVersion;
import ar.edu.itba.certiflow.assistance.model.ProposedFinding;
import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.assistance.model.exceptions.AssistantUnavailableException;
import ar.edu.itba.certiflow.harness.golden.CaseInspections;
import ar.edu.itba.certiflow.harness.golden.GoldenCase;
import ar.edu.itba.certiflow.harness.golden.GoldenSet;
import ar.edu.itba.certiflow.infrastructure.assistance.StubInspectionAssistant;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Las métricas sobre salidas conocidas: un asistente con respuestas fijas y el stub, que es determinista. */
class HarnessRunTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);
    private final List<GoldenCase> cases = new GoldenSet().cases();
    private final GoldenCase twoNonConformities = cases.get(9);
    private final List<String> progress = new ArrayList<>();

    private HarnessRun harness(InspectionAssistant assistant) {
        return new HarnessRun(assistant, new CaseInspections(clock), clock, progress::add);
    }

    private ProposedFinding proposal(String criterion, StandardSeverity severity, String quote) {
        return new ProposedFinding(new CriterionCode(criterion), severity, "Because of " + criterion, List.of(new Quote(quote)));
    }

    /**
     * Sobre el caso 10, que espera RELIEF-VALVE CRITICAL y PANEL MINOR: acierta la válvula con su severidad, propone
     * PANEL con una cita que no está en el texto (la frena un guardrail), inventa LEAKS y repite la válvula.
     */
    private final InspectionAssistant scripted = inspection -> new AssistantReply(new PromptVersion("v1"),
            new ModelId("scripted-model"), List.of(
            proposal("RELIEF-VALVE", StandardSeverity.CRITICAL, "la válvula recién abrió a 9,2 bar"),
            proposal("RELIEF-VALVE", StandardSeverity.MINOR, "abrió a 9,2 bar"),
            proposal("PANEL", StandardSeverity.MINOR, "el tablero no tiene rótulos"),
            proposal("LEAKS", StandardSeverity.MAJOR, "Faltan las etiquetas")));

    @Test
    void metricsCountCriteriaAfterTheGuardrailsAndKeepWhatTheAssistantSaid() {
        HarnessReport report = harness(scripted).run(List.of(twoNonConformities), 3);
        CaseResult result = report.cases().getFirst();
        RunResult run = result.runs().getFirst();

        assertEquals(List.of(1, 1, 1, 1), List.of(run.correct(), run.severityHits(), run.invented(), run.missed()));
        assertEquals(List.of("RELIEF-VALVE", "LEAKS"),
                run.suggestions().stream().map(RunResult.Proposal::criterion).toList());
        assertEquals(List.of("DUPLICATE_CRITERION", "UNSUPPORTED_QUOTE"), run.rejected().stream().map(RunResult.Rejection::guardrail).toList());
        assertEquals(new Statistic(1.0, 1, 1), result.statistics().get(Metric.CORRECT));
        assertEquals(new Statistic(1.0, 1, 1), report.totals().get(Metric.INVENTED));
        assertEquals(Map.of("DUPLICATE_CRITERION", 3, "UNSUPPORTED_QUOTE", 3), report.rejectedByGuardrail());
        assertEquals(List.of("v1", "scripted-model", "RELIEF-VALVE CRITICAL, PANEL MINOR"),
                List.of(report.promptVersion(), report.model(), String.join(", ", result.expected())));
        assertEquals(List.of("case-10: 3 of 3 runs measured"), progress);
    }

    @Test
    void aRunTheProviderCouldNotAnswerIsAFailureAndIsNotMeasured() {
        AtomicInteger calls = new AtomicInteger();
        InspectionAssistant flaky = inspection -> {
            if (calls.incrementAndGet() == 2) {
                throw new AssistantUnavailableException("Provider unavailable",
                        new IllegalStateException("504 Gateway Timeout"));
            }
            return scripted.review(inspection);
        };

        CaseResult result = harness(flaky).run(List.of(twoNonConformities), 3).cases().getFirst();

        assertEquals(List.of(1, 3), result.runs().stream().map(RunResult::run).toList());
        assertEquals(List.of("run 2: 504 Gateway Timeout"), result.failures());
        assertEquals(new Statistic(1.0, 1, 1), result.statistics().get(Metric.MISSED));
    }

    @Test
    void aHarnessWithoutASingleAnswerHasNothingToReport() {
        InspectionAssistant down = inspection -> {
            throw new AssistantUnavailableException("Provider unavailable");
        };

        assertThrows(IllegalStateException.class, () -> harness(down).run(List.of(twoNonConformities), 2));
    }

    /**
     * La línea de base del stub, exacta porque es determinista: acierta las fallas con palabras obvias, falla las
     * descripciones sin ellas y los problemas anotados en otro criterio, y cae en la trampa de la negación.
     */
    @Test
    void theStubBaselineIsExactOnTheWholeGoldenSet() {
        HarnessReport report = harness(new StubInspectionAssistant()).run(cases, 2);

        assertEquals(Map.of(Metric.CORRECT, new Statistic(4.0, 4, 4), Metric.SEVERITY_HITS, new Statistic(3.0, 3, 3),
                Metric.INVENTED, new Statistic(2.0, 2, 2), Metric.MISSED, new Statistic(7.0, 7, 7)), report.totals());
        assertEquals(Map.of("ALREADY_NONCONFORMING", 2), report.rejectedByGuardrail());
        assertEquals(List.of("case-06", "case-14"), report.cases().stream()
                .filter(result -> result.statistics().get(Metric.INVENTED).mean() > 0).map(CaseResult::id).toList());
    }
}
