package ar.edu.itba.certiflow.harness;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Las corridas de un caso con su media y rango por métrica. Una corrida en la que el proveedor falló no se mide:
 * queda en {@code failures}, porque contarla como "sin sugerencias" mejoraría los inventados sin motivo. Un caso sin
 * ninguna corrida medida no tiene estadísticas.
 */
public record CaseResult(String id, String kind, String description, List<String> expected,
                         Map<Metric, Statistic> statistics, List<RunResult> runs, List<String> failures, String inputFingerprint) {

    public CaseResult {
        expected = List.copyOf(expected);
        EnumMap<Metric, Statistic> ordered = new EnumMap<>(Metric.class);
        ordered.putAll(statistics);
        statistics = Collections.unmodifiableMap(ordered);
        runs = List.copyOf(runs);
        failures = List.copyOf(failures);
    }

    public CaseResult(String id, String kind, String description, List<String> expected,
                      Map<Metric, Statistic> statistics, List<RunResult> runs, List<String> failures) {
        this(id, kind, description, expected, statistics, runs, failures, null);
    }

    public CaseResult(String id, String kind, String description, List<String> expected, List<RunResult> runs,
                      List<String> failures, String inputFingerprint) {
        Map<Metric, Statistic> statistics = runs.isEmpty() ? Map.of() : Arrays.stream(Metric.values()).collect(Collectors.toMap(Function.identity(),
                metric -> new Statistic(runs.stream().map(metric::of).toList()), (first, second) -> first,
                () -> new EnumMap<>(Metric.class)));
        this(id, kind, description, expected, statistics, runs, failures, inputFingerprint);
    }

    public CaseResult(String id, String kind, String description, List<String> expected, List<RunResult> runs,
                      List<String> failures) {
        this(id, kind, description, expected, runs, failures, null);
    }
}
