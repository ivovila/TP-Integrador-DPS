package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.Cancellation;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionSnapshot;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "corrective_action")
@IdClass(CorrectiveActionEntity.Key.class)
public class CorrectiveActionEntity {

    @Id
    @Column(name = "finding_id")
    private String findingId;
    @Id
    private int number;
    private String description;
    @Column(name = "responsible_id")
    private String responsibleId;
    @Column(name = "responsible_name")
    private String responsibleName;
    @Column(name = "due_date")
    private LocalDate dueDate;
    @Column(name = "planned_by_id")
    private String plannedById;
    @Column(name = "planned_by_name")
    private String plannedByName;
    @Column(name = "planned_at")
    private Instant plannedAt;
    @Column(name = "cancelled_by_id")
    private String cancelledById;
    @Column(name = "cancelled_by_name")
    private String cancelledByName;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "verification", joinColumns = {
            @JoinColumn(name = "finding_id", referencedColumnName = "finding_id"),
            @JoinColumn(name = "action_number", referencedColumnName = "number")})
    @OrderColumn(name = "position")
    private List<VerificationEmbeddable> verifications = new ArrayList<>();

    public static class Key implements Serializable {

        private String findingId;
        private int number;

        public Key() {
        }

        public Key(String findingId, int number) {
            this.findingId = findingId;
            this.number = number;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && number == key.number && Objects.equals(findingId, key.findingId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(findingId, number);
        }
    }

    protected CorrectiveActionEntity() {
    }

    static CorrectiveActionEntity from(String findingId, CorrectiveActionSnapshot snapshot) {
        CorrectiveActionEntity entity = new CorrectiveActionEntity();
        entity.findingId = findingId;
        entity.number = snapshot.id().value();
        PersonColumns responsible = PersonColumns.of(snapshot.responsible());
        PersonColumns plannedBy = PersonColumns.of(snapshot.plannedBy());
        entity.description = snapshot.description();
        entity.responsibleId = responsible.id();
        entity.responsibleName = responsible.name();
        entity.dueDate = snapshot.dueDate();
        entity.plannedById = plannedBy.id();
        entity.plannedByName = plannedBy.name();
        entity.plannedAt = snapshot.plannedAt();
        entity.copyFrom(snapshot);
        return entity;
    }

    void copyFrom(CorrectiveActionSnapshot snapshot) {
        verifications.clear();
        snapshot.verifications().forEach(verification -> verifications.add(VerificationEmbeddable.of(verification)));
        Optional<PersonColumns> cancelledBy = snapshot.cancellation().map(cancellation -> PersonColumns.of(cancellation.cancelledBy()));
        cancelledById = cancelledBy.map(PersonColumns::id).orElse(null);
        cancelledByName = cancelledBy.map(PersonColumns::name).orElse(null);
        cancelledAt = snapshot.cancellation().map(Cancellation::cancelledAt).orElse(null);
    }

    CorrectiveActionSnapshot toSnapshot() {
        return new CorrectiveActionSnapshot(new CorrectiveActionId(number), description,
                new PersonColumns(responsibleId, responsibleName).toDomain(), dueDate,
                new Stamp(new PersonColumns(plannedById, plannedByName).toDomain(), plannedAt),
                verifications.stream().map(VerificationEmbeddable::toDomain).toList(),
                Optional.ofNullable(cancelledAt).map(at -> new Cancellation(new Stamp(new PersonColumns(cancelledById, cancelledByName).toDomain(), at))));
    }
}
