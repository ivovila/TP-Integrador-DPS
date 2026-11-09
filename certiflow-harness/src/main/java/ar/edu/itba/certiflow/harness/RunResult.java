package ar.edu.itba.certiflow.harness;

import java.util.List;

/**
 * Una corrida de un caso, medida sobre las sugerencias que pasaron los guardrails, con lo que el asistente dijo:
 * sin el detalle no se puede ver qué errores comete ni escribir la versión siguiente del prompt.
 */
public record RunResult(int run, int correct, int severityHits, int invented, int missed, List<Proposal> suggestions,
                        List<Rejection> rejected) {

    /** Lo que propuso el asistente para un criterio. */
    public record Proposal(String criterion, String severity, String rationale, List<String> quotes) {
    }

    /** Una propuesta que frenó un guardrail, y cuál. */
    public record Rejection(Proposal proposal, String guardrail) {
    }

    public RunResult {
        suggestions = List.copyOf(suggestions);
        rejected = List.copyOf(rejected);
    }
}
