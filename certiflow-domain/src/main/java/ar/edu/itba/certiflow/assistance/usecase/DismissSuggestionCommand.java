package ar.edu.itba.certiflow.assistance.usecase;

import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.shared.model.Person;

public record DismissSuggestionCommand(ReviewId reviewId, int position, String reason, Person dismissedBy) {
}