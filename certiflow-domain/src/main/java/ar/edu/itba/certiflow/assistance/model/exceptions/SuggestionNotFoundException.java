package ar.edu.itba.certiflow.assistance.model.exceptions;

import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public class SuggestionNotFoundException extends NotFoundException {

    public SuggestionNotFoundException(ReviewId reviewId, int position) {
        super("Assistant review " + reviewId.value() + " has no suggestion " + position);
    }
}
