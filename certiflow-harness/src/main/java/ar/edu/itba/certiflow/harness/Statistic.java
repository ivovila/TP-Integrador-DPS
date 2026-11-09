package ar.edu.itba.certiflow.harness;

import java.util.IntSummaryStatistics;
import java.util.List;

/** Media y rango de una métrica a lo largo de las corridas: un modelo no da dos veces lo mismo. */
public record Statistic(double mean, int min, int max) {

    public Statistic(List<Integer> values) {
        IntSummaryStatistics summary = values.stream().mapToInt(Integer::intValue).summaryStatistics();
        if (summary.getCount() == 0) {
            throw new IllegalArgumentException("A statistic needs at least one measured run");
        }
        this(summary.getAverage(), summary.getMin(), summary.getMax());
    }
}
