package ar.edu.itba.certiflow.assistance.model;

/** No se reconstruye historia inventando qué pudo haber leído un modelo antes de capturar entradas. */
public record UnavailableReviewInput() implements ReviewInput {
    @Override
    public <T> T describedBy(ReviewInputDescriber<T> describer) {
        return describer.unavailable();
    }
}
