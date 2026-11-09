package ar.edu.itba.certiflow.assistance.model;

/** La inspección traducida que se le mandó al modelo, tal como estaba al pedir la revisión. */
public record CapturedReviewInput(InspectionUnderReview inspection) implements ReviewInput {

    @Override
    public <T> T describedBy(ReviewInputDescriber<T> describer) {
        return describer.captured(inspection);
    }
}
