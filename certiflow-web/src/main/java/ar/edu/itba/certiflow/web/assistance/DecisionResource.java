package ar.edu.itba.certiflow.web.assistance;

import ar.edu.itba.certiflow.assistance.model.Acceptance;
import ar.edu.itba.certiflow.assistance.model.Dismissal;
import ar.edu.itba.certiflow.assistance.model.SuggestionDecisionDescriber;
import ar.edu.itba.certiflow.web.support.ResourceNotFoundException;

/** La existencia del subrecurso se decide sobre la variante de dominio, antes de armar el DTO. */
enum DecisionResource implements SuggestionDecisionDescriber<DecisionResponse> {
    ACCEPTANCE {
        @Override
        public DecisionResponse accepted(Acceptance acceptance) {
            return new DecisionResponse.Describer().accepted(acceptance);
        }
    },
    DISMISSAL {
        @Override
        public DecisionResponse dismissed(Dismissal dismissal) {
            return new DecisionResponse.Describer().dismissed(dismissal);
        }
    };

    @Override
    public DecisionResponse pending() { throw missing(); }
    @Override
    public DecisionResponse accepted(Acceptance acceptance) { throw missing(); }
    @Override
    public DecisionResponse dismissed(Dismissal dismissal) { throw missing(); }

    private ResourceNotFoundException missing() {
        return new ResourceNotFoundException("Suggestion has no " + name().toLowerCase(java.util.Locale.ROOT));
    }
}
