package ar.edu.itba.certiflow.harness;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El resultado de correr el conjunto dorado con una versión de prompt y un modelo. Los totales suman los casos de
 * cada corrida (la corrida 1 de todos, la 2 de todos…) y dan la media y el rango de esas sumas.
 */
public record HarnessReport(String promptVersion, String model, int runsPerCase, Instant ranAt,
                            Map<Metric, Statistic> totals, Map<String, Integer> rejectedByGuardrail, List<CaseResult> cases) {

    public HarnessReport {
        EnumMap<Metric, Statistic> ordered = new EnumMap<>(Metric.class);
        ordered.putAll(totals);
        totals = Collections.unmodifiableMap(ordered);
        rejectedByGuardrail = Collections.unmodifiableMap(new TreeMap<>(rejectedByGuardrail));
        cases = List.copyOf(cases);
    }

    public HarnessReport(String promptVersion, String model, int runsPerCase, Instant ranAt, List<CaseResult> cases) {
        List<Integer> complete = new CompleteRepetitions(cases, runsPerCase).numbers();
        Map<Metric, Statistic> totals = Arrays.stream(Metric.values())
                .filter(metric -> !complete.isEmpty())
                .collect(Collectors.toMap(Function.identity(),
                        metric -> new Statistic(complete.stream()
                                .map(run -> cases.stream().flatMap(result -> result.runs().stream())
                                        .filter(result -> result.run() == run).mapToInt(metric::of).sum()).toList()),
                        (first, second) -> first, () -> new EnumMap<>(Metric.class)));
        Map<String, Integer> rejected = cases.stream().flatMap(result -> result.runs().stream())
                .flatMap(run -> run.rejected().stream())
                .collect(Collectors.groupingBy(RunResult.Rejection::guardrail, TreeMap::new, Collectors.summingInt(rejection -> 1)));
        this(promptVersion, model, runsPerCase, ranAt, totals, rejected, cases);
    }

    public int completeRuns() {
        return new CompleteRepetitions(cases, runsPerCase).numbers().size();
    }

    /**
     * Solo se compara un reporte sin fallas, con todas sus repeticiones completas y con la huella de cada caso: uno
     * anterior a las huellas no dice con qué entradas se midió.
     */
    public boolean isComparable() {
        return failures() == 0 && !cases.isEmpty() && completeRuns() == runsPerCase
                && cases.stream().allMatch(result -> result.isComparable(runsPerCase));
    }

    public Map<String, String> inputFingerprints() {
        return cases.stream().collect(Collectors.toMap(CaseResult::id, CaseResult::inputFingerprint));
    }

    public int failures() {
        return cases.stream().mapToInt(result -> result.failures().size()).sum();
    }
}
