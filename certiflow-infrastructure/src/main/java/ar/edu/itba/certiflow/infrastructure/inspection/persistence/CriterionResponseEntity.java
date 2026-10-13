package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
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

    CriterionResponseEntity(CriterionResponse<?> response) {
        copyFrom(response);
    }

    void copyFrom(CriterionResponse<?> response) {
        criterionCode = response.criterion().code().value();
        StoredAnswering answering = response.answering().describedBy(new StoredAnsweringDescriber<>());
        answerType = answering.answerType();
        answerValue = answering.answerValue();
        answeredById = answering.answeredById();
        answeredByName = answering.answeredByName();
        answeredAt = answering.answeredAt();
        evidence.clear();
        response.attachments().forEach(attached -> evidence.add(new EvidenceEmbeddable(attached)));
        observations.clear();
        response.observations().forEach(observation -> observations.add(new ObservationEmbeddable(observation)));
    }

    CriterionResponse<?> toDomain(SchemaVersion version, EvidenceKindCatalog evidenceKinds) {
        return new StoredAnswering(answerType, answerValue, answeredById, answeredByName, answeredAt)
                .responseTo(version.criterion(new CriterionCode(criterionCode)),
                        evidence.stream().map(attached -> attached.toDomain(evidenceKinds)).toList(),
                        observations.stream().map(ObservationEmbeddable::toDomain).toList());
    }
}
