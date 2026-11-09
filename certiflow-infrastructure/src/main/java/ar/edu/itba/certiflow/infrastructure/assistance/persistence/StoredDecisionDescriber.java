package ar.edu.itba.certiflow.infrastructure.assistance.persistence;

import ar.edu.itba.certiflow.assistance.model.Acceptance;
import ar.edu.itba.certiflow.assistance.model.Dismissal;
import ar.edu.itba.certiflow.assistance.model.SuggestionDecisionDescriber;
import java.util.stream.Stream;

/** La fila de la decisión de una sugerencia: ninguna mientras está pendiente. */
final class StoredDecisionDescriber implements SuggestionDecisionDescriber<Stream<SuggestionDecisionEmbeddable>> {

    private final int position;

    StoredDecisionDescriber(int position) {
        this.position = position;
    }

    @Override
    public Stream<SuggestionDecisionEmbeddable> pending() {
        return Stream.empty();
    }

    @Override
    public Stream<SuggestionDecisionEmbeddable> accepted(Acceptance acceptance) {
        return Stream.of(new SuggestionDecisionEmbeddable(position, StoredDecisionKind.ACCEPTED, acceptance.accepted(), null));
    }

    @Override
    public Stream<SuggestionDecisionEmbeddable> dismissed(Dismissal dismissal) {
        return Stream.of(new SuggestionDecisionEmbeddable(position, StoredDecisionKind.DISMISSED, dismissal.dismissed(),
                dismissal.reason()));
    }
}
