package ar.edu.itba.certiflow.web.assistance;

import ar.edu.itba.certiflow.assistance.model.Acceptance;
import ar.edu.itba.certiflow.assistance.model.Dismissal;
import ar.edu.itba.certiflow.assistance.model.SuggestionDecision;
import ar.edu.itba.certiflow.assistance.model.SuggestionDecisionDescriber;
import ar.edu.itba.certiflow.web.support.ResourceNotFoundException;
import java.util.Locale;
import java.util.Optional;

/**
 * Los subrecursos {@code acceptance} y {@code dismissal}: cada uno existe solo para su variante de la decisión. El
 * receptor dice si la decisión es la suya y no lanza (D26); el 404 lo decide {@link #of}.
 */
enum DecisionResource implements SuggestionDecisionDescriber<Optional<DecisionResponse>> {
    ACCEPTANCE {
        @Override
        public Optional<DecisionResponse> accepted(Acceptance acceptance) {
            return Optional.of(new DecisionResponse.Describer().accepted(acceptance));
        }
    },
    DISMISSAL {
        @Override
        public Optional<DecisionResponse> dismissed(Dismissal dismissal) {
            return Optional.of(new DecisionResponse.Describer().dismissed(dismissal));
        }
    };

    @Override
    public Optional<DecisionResponse> pending() {
        return Optional.empty();
    }

    @Override
    public Optional<DecisionResponse> accepted(Acceptance acceptance) {
        return Optional.empty();
    }

    @Override
    public Optional<DecisionResponse> dismissed(Dismissal dismissal) {
        return Optional.empty();
    }

    DecisionResponse of(SuggestionDecision decision) {
        return decision.describedBy(this)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion has no " + name().toLowerCase(Locale.ROOT)));
    }
}
