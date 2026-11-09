package ar.edu.itba.certiflow.assistance.model.exceptions;

import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public class AssistantReviewNotFoundException extends NotFoundException {

    public AssistantReviewNotFoundException(ReviewId id) {
        super("Assistant review " + id.value() + " does not exist");
    }
}
