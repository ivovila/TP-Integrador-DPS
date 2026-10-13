package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.Instants;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
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
import org.springframework.stereotype.Component;

/** La evaluación sellada en una revisión, guardada como un documento JSON dentro de la fila de la revisión. */
@Component
public class EvaluationDocumentMapper {

    record ResponseDocument(String criterionCode, String answerType, String answerValue, String answeredById,
                            String answeredByName, Instant answeredAt, List<EvidenceDocument> evidence,
                            List<ObservationDocument> observations) {
    }

    record EvidenceDocument(String kind, String reference, String description, String attachedById, String attachedByName,
                            Instant attachedAt) {
    }

    record ObservationDocument(String text, String observedById, String observedByName, Instant observedAt) {
    }

    private final StoredJson storedJson;

    public EvaluationDocumentMapper(StoredJson storedJson) {
        this.storedJson = storedJson;
    }

    String write(Evaluation evaluation) {
        return storedJson.write(evaluation.responses().stream().map(this::responseDocumentOf).toList());
    }

    Evaluation read(String storedEvaluation, SchemaVersion version) {
        List<ResponseDocument> documents = storedJson.readListOf(storedEvaluation, ResponseDocument.class);
        return new Evaluation(documents.stream()
                .<CriterionResponse<?>>map(document -> responseOf(document, version))
                .toList());
    }

    private ResponseDocument responseDocumentOf(CriterionResponse<?> response) {
        StoredAnswering answering = response.answering().describedBy(new StoredAnsweringDescriber<>());
        return new ResponseDocument(response.criterion().code(), answering.answerType(), answering.answerValue(),
                answering.answeredById(), answering.answeredByName(), Instants.asStored(answering.answeredAt()),
                response.attachments().stream().map(this::evidenceDocumentOf).toList(),
                response.observations().stream().map(this::observationDocumentOf).toList());
    }

    private EvidenceDocument evidenceDocumentOf(AttachedEvidence attached) {
        PersonColumns attachedBy = new PersonColumns(attached.attachedBy());
        return new EvidenceDocument(attached.evidence().kind().code(), attached.evidence().reference(),
                attached.evidence().description(), attachedBy.id(), attachedBy.name(),
                Instants.asStored(attached.attachedAt()));
    }

    private ObservationDocument observationDocumentOf(Observation observation) {
        PersonColumns observedBy = new PersonColumns(observation.observedBy());
        return new ObservationDocument(observation.text(), observedBy.id(), observedBy.name(),
                Instants.asStored(observation.observedAt()));
    }

    private CriterionResponse<?> responseOf(ResponseDocument document, SchemaVersion version) {
        StoredAnswering answering = new StoredAnswering(document.answerType(), document.answerValue(),
                document.answeredById(), document.answeredByName(), document.answeredAt());
        return answering.responseTo(version.criterion(document.criterionCode()),
                document.evidence().stream().map(this::attachedEvidenceOf).toList(),
                document.observations().stream().map(this::observationOf).toList());
    }

    private AttachedEvidence attachedEvidenceOf(EvidenceDocument document) {
        return new AttachedEvidence(
                new Evidence(StandardEvidenceKind.valueOf(document.kind()), document.reference(), document.description()),
                new Stamp(new PersonColumns(document.attachedById(), document.attachedByName()).toDomain(),
                        document.attachedAt()));
    }

    private Observation observationOf(ObservationDocument document) {
        return new Observation(document.text(),
                new Stamp(new PersonColumns(document.observedById(), document.observedByName()).toDomain(),
                        document.observedAt()));
    }
}
