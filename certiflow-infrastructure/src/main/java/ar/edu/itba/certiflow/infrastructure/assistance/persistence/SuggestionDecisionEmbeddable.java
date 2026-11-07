package ar.edu.itba.certiflow.infrastructure.assistance.persistence;

import ar.edu.itba.certiflow.assistance.model.SuggestionDecision;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.shared.model.Stamp;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class SuggestionDecisionEmbeddable {

    private int position;
    private String kind;
    @Column(name = "decided_by_id")
    private String decidedById;
    @Column(name = "decided_by_name")
    private String decidedByName;
    @Column(name = "decided_at")
    private Instant decidedAt;
    private String reason;

    protected SuggestionDecisionEmbeddable() {
    }

    SuggestionDecisionEmbeddable(int position, StoredDecisionKind kind, Stamp decided, String reason) {
        PersonColumns decidedBy = new PersonColumns(decided.author());
        this.position = position;
        this.kind = kind.name();
        this.decidedById = decidedBy.id();
        this.decidedByName = decidedBy.name();
        this.decidedAt = decided.instant();
        this.reason = reason;
    }

    boolean isFor(int suggestionPosition) {
        return position == suggestionPosition;
    }

    SuggestionDecision toDomain() {
        return StoredDecisionKind.valueOf(kind)
                .decision(new Stamp(new PersonColumns(decidedById, decidedByName).toDomain(), decidedAt), reason);
    }
}
