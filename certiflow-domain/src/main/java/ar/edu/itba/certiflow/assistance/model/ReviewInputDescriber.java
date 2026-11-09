package ar.edu.itba.certiflow.assistance.model;

public interface ReviewInputDescriber<T> {
    T captured(InspectionUnderReview inspection);
    T unavailable();
}
