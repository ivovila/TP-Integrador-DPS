package ar.edu.itba.certiflow.web.assistance;

import ar.edu.itba.certiflow.assistance.model.Acceptance;
import ar.edu.itba.certiflow.assistance.model.Dismissal;
import ar.edu.itba.certiflow.assistance.model.SuggestionDecisionDescriber;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.time.Instant;

public record DecisionResponse(Status status, PersonPayload decidedBy, Instant decidedAt, String reason) {

    public enum Status { PENDING, ACCEPTED, DISMISSED }

    static final class Describer implements SuggestionDecisionDescriber<DecisionResponse> {

        @Override
        public DecisionResponse pending() {
            return new DecisionResponse(Status.PENDING, null, null, null);
        }

        @Override
        public DecisionResponse accepted(Acceptance acceptance) {
            return new DecisionResponse(Status.ACCEPTED, PersonPayload.of(acceptance.accepted().author()), acceptance.accepted().instant(), null);
        }

        @Override
        public DecisionResponse dismissed(Dismissal dismissal) {
            return new DecisionResponse(Status.DISMISSED, PersonPayload.of(dismissal.dismissed().author()),
                    dismissal.dismissed().instant(), dismissal.reason());
        }
    }
}
