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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
        Optional<AnswerColumns> answer = response.given().map(given -> AnswerColumns.of(given.value().raw()));
        Optional<PersonColumns> by = response.given().map(given -> PersonColumns.of(given.answeredBy()));
        return new ResponseDocument(response.criterion().code(),
                answer.map(AnswerColumns::type).orElse(null), answer.map(AnswerColumns::value).orElse(null),
                by.map(PersonColumns::id).orElse(null), by.map(PersonColumns::name).orElse(null),
                response.given().map(given -> Instants.asStored(given.answeredAt())).orElse(null),
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
                Optional.ofNullable(document.answerType()).map(type -> new AnswerColumns(type, document.answerValue())),
                new PersonColumns(document.answeredById(), document.answeredByName()), document.answeredAt(),
                document.evidence().stream()
                        .map(evidence -> new AttachedEvidence(
                                new Evidence(StandardEvidenceKind.valueOf(evidence.kind()), evidence.reference(), evidence.description()),
                                new PersonColumns(evidence.attachedById(), evidence.attachedByName()).toDomain(), evidence.attachedAt()))
                        .toList(),
                document.observations().stream()
                        .map(observation -> new Observation(observation.text(),
                                new PersonColumns(observation.observedById(), observation.observedByName()).toDomain(),
                                observation.observedAt()))
                        .toList());
    }
}
