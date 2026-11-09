package ar.edu.itba.certiflow.assistance.model;

import java.util.Objects;

public record CapturedReviewInput(InspectionUnderReview inspection) implements ReviewInput {
    public CapturedReviewInput {
        Objects.requireNonNull(inspection, "inspection");
    }
    @Override
    public <T> T describedBy(ReviewInputDescriber<T> describer) {
        return describer.captured(inspection);
    }
}
