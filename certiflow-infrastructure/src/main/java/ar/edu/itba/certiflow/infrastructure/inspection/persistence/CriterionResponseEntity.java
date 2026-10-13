package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "criterion_response")
public class CriterionResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "criterion_code")
    private String criterionCode;
    @Column(name = "answer_type")
    private String answerType;
    @Column(name = "answer_value")
    private String answerValue;
    @Column(name = "answered_by_id")
    private String answeredById;
    @Column(name = "answered_by_name")
    private String answeredByName;
    @Column(name = "answered_at")
    private Instant answeredAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "attached_evidence", joinColumns = @JoinColumn(name = "response_id"))
    @OrderColumn(name = "position")
    private List<EvidenceEmbeddable> evidence = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "observation", joinColumns = @JoinColumn(name = "response_id"))
    @OrderColumn(name = "position")
    private List<ObservationEmbeddable> observations = new ArrayList<>();

    protected CriterionResponseEntity() {
    }

    static CriterionResponseEntity from(CriterionResponse<?> response) {
        CriterionResponseEntity entity = new CriterionResponseEntity();
        entity.copyFrom(response);
        return entity;
    }

    void copyFrom(CriterionResponse<?> response) {
        criterionCode = response.criterion().code();
        answerType = null;
        answerValue = null;
        answeredById = null;
        answeredByName = null;
        answeredAt = null;
        response.given().ifPresent(given -> {
            AnswerColumns answer = AnswerColumns.of(given.value().raw());
            PersonColumns by = PersonColumns.of(given.answeredBy());
            answerType = answer.type();
            answerValue = answer.value();
            answeredById = by.id();
            answeredByName = by.name();
            answeredAt = given.answeredAt();
        });
        evidence.clear();
        response.attachments().forEach(attached -> evidence.add(EvidenceEmbeddable.of(attached)));
        observations.clear();
        response.observations().forEach(observation -> observations.add(ObservationEmbeddable.of(observation)));
    }

    CriterionResponse<?> toDomain(SchemaVersion version) {
        return ResponseRebuilder.rebuild(version.criterion(criterionCode),
                answerType, answerValue,
                new PersonColumns(answeredById, answeredByName), answeredAt,
                evidence.stream().map(EvidenceEmbeddable::toDomain).toList(),
                observations.stream().map(ObservationEmbeddable::toDomain).toList());
    }
}
