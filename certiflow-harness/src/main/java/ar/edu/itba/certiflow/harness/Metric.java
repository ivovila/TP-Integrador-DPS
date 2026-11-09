package ar.edu.itba.certiflow.harness;

/** Las métricas de la consigna, más las omitidas: sin ellas, un asistente que no sugiere nada parece perfecto. */
public enum Metric {

    CORRECT("Hallazgos correctos", Direction.HIGHER_IS_BETTER) {
        @Override
        public int of(RunResult run) {
            return run.correct();
        }
    },
    SEVERITY_HITS("Severidades acertadas", Direction.HIGHER_IS_BETTER) {
        @Override
        public int of(RunResult run) {
            return run.severityHits();
        }
    },
    INVENTED("Hallazgos inventados", Direction.LOWER_IS_BETTER) {
        @Override
        public int of(RunResult run) {
            return run.invented();
        }
    },
    MISSED("Omitidos", Direction.LOWER_IS_BETTER) {
        @Override
        public int of(RunResult run) {
            return run.missed();
        }
    };

    /**
     * Hacia dónde mejora una métrica. Empeora cuando la media nueva queda fuera del rango que se observó antes: una
     * diferencia dentro de ese rango es el ruido de un modelo que no responde siempre igual.
     */
    private enum Direction {
        HIGHER_IS_BETTER {
            @Override
            boolean worsened(Statistic baseline, Statistic candidate) {
                return candidate.mean() < baseline.min();
            }
        },
        LOWER_IS_BETTER {
            @Override
            boolean worsened(Statistic baseline, Statistic candidate) {
                return candidate.mean() > baseline.max();
            }
        };

        abstract boolean worsened(Statistic baseline, Statistic candidate);
    }

    private final String label;
    private final Direction direction;

    Metric(String label, Direction direction) {
        this.label = label;
        this.direction = direction;
    }

    public abstract int of(RunResult run);

    public String label() {
        return label;
    }

    public boolean worsened(Statistic baseline, Statistic candidate) {
        return direction.worsened(baseline, candidate);
    }
}
