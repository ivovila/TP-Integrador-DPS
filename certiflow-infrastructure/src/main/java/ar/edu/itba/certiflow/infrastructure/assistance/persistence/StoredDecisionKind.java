package ar.edu.itba.certiflow.infrastructure.assistance.persistence;

import ar.edu.itba.certiflow.assistance.model.Acceptance;
import ar.edu.itba.certiflow.assistance.model.Dismissal;
import ar.edu.itba.certiflow.assistance.model.SuggestionDecision;
import ar.edu.itba.certiflow.shared.model.Stamp;

/** Las decisiones que tienen fila; una sugerencia pendiente no tiene ninguna. */
enum StoredDecisionKind {

    ACCEPTED {
        @Override
        SuggestionDecision decision(Stamp decided, String reason) {
            return new Acceptance(decided);
        }
    },
    DISMISSED {
        @Override
        SuggestionDecision decision(Stamp decided, String reason) {
            return new Dismissal(decided, reason);
        }
    };

    abstract SuggestionDecision decision(Stamp decided, String reason);
}
