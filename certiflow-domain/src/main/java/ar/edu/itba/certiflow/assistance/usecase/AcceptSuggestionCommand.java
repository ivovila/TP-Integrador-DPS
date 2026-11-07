package ar.edu.itba.certiflow.assistance.usecase;

import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.shared.model.Person;

public record AcceptSuggestionCommand(ReviewId reviewId, int position, Person acceptedBy) {
}