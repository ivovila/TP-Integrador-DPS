package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.Instants;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.AttachedEvidence;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.Evaluation;
import ar.edu.itba.certiflow.inspection.model.Observation;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.List;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

final class EvaluationJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    record ResponseDocument(String criterionCode, String answerType, String answerValue, String answeredById,
                            String answeredByName, Instant answeredAt, List<EvidenceDocument> evidence,
                            List<ObservationDocument> observations) {
    }

    record EvidenceDocument(String kind, String reference, String description, String attachedById, String attachedByName,
                            Instant attachedAt) {
    }

    record ObservationDocument(String text, String observedById, String observedByName, Instant observedAt) {
    }

    private EvaluationJson() {
    }

    static String write(Evaluation evaluation) {
        return MAPPER.writeValueAsString(evaluation.responses().stream().map(EvaluationJson::document).toList());
    }

    static Evaluation read(String json, SchemaVersion version) {
        List<ResponseDocument> documents = MAPPER.readValue(json, new TypeReference<List<ResponseDocument>>() {
        });
        return new Evaluation(documents.stream().<CriterionResponse<?>>map(document -> response(document, version)).toList());
    }

    private static ResponseDocument document(CriterionResponse<?> response) {
        StoredAnswering answering = response.answering().describedBy(new StoredAnsweringDescriber<>());
        return new ResponseDocument(response.criterion().code(), answering.answerType(), answering.answerValue(),
                answering.answeredById(), answering.answeredByName(), Instants.asStored(answering.answeredAt()),
                response.attachments().stream().map(EvaluationJson::document).toList(),
                response.observations().stream().map(EvaluationJson::document).toList());
    }

    private static EvidenceDocument document(AttachedEvidence attached) {
        PersonColumns by = PersonColumns.of(attached.attachedBy());
        return new EvidenceDocument(attached.evidence().kind().code(), attached.evidence().reference(),
                attached.evidence().description(), by.id(), by.name(), Instants.asStored(attached.attachedAt()));
    }

    private static ObservationDocument document(Observation observation) {
        PersonColumns by = PersonColumns.of(observation.observedBy());
        return new ObservationDocument(observation.text(), by.id(), by.name(), Instants.asStored(observation.observedAt()));
    }

    private static CriterionResponse<?> response(ResponseDocument document, SchemaVersion version) {
        return ResponseRebuilder.rebuild(version.criterion(document.criterionCode()),
                new StoredAnswering(document.answerType(), document.answerValue(), document.answeredById(),
                        document.answeredByName(), document.answeredAt()),
                document.evidence().stream()
                        .map(evidence -> new AttachedEvidence(
                                new Evidence(StandardEvidenceKind.valueOf(evidence.kind()), evidence.reference(), evidence.description()),
                                new Stamp(new PersonColumns(evidence.attachedById(), evidence.attachedByName()).toDomain(), evidence.attachedAt())))
                        .toList(),
                document.observations().stream()
                        .map(observation -> new Observation(observation.text(),
                                new Stamp(new PersonColumns(observation.observedById(), observation.observedByName()).toDomain(),
                                        observation.observedAt())))
                        .toList());
    }
}
