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
import java.util.stream.IntStream;

/**
 * El resultado de correr el conjunto dorado con una versión de prompt y un modelo. Los totales suman los casos de
 * cada corrida (la corrida 1 de todos, la 2 de todos…) y dan la media y el rango de esas sumas.
 */
public record HarnessReport(String promptVersion, String model, int runsPerCase, Instant ranAt,
                            Map<Metric, Statistic> totals, Map<String, Integer> rejectedByGuardrail, List<CaseResult> cases) {

    public HarnessReport {
        totals = Collections.unmodifiableMap(new EnumMap<>(totals));
        rejectedByGuardrail = Collections.unmodifiableMap(new TreeMap<>(rejectedByGuardrail));
        cases = List.copyOf(cases);
    }

    public HarnessReport(String promptVersion, String model, int runsPerCase, Instant ranAt, List<CaseResult> cases) {
        Map<Metric, Statistic> totals = Arrays.stream(Metric.values()).collect(Collectors.toMap(Function.identity(),
                metric -> new Statistic(IntStream.rangeClosed(1, runsPerCase)
                        .mapToObj(run -> cases.stream().flatMap(result -> result.runs().stream())
                                .filter(result -> result.run() == run).mapToInt(metric::of).sum())
                        .toList()),
                (first, second) -> first, () -> new EnumMap<>(Metric.class)));
        Map<String, Integer> rejected = cases.stream().flatMap(result -> result.runs().stream())
                .flatMap(run -> run.rejected().stream())
                .collect(Collectors.groupingBy(RunResult.Rejection::guardrail, TreeMap::new, Collectors.summingInt(rejection -> 1)));
        this(promptVersion, model, runsPerCase, ranAt, totals, rejected, cases);
    }

    public int failures() {
        return cases.stream().mapToInt(result -> result.failures().size()).sum();
    }
}
