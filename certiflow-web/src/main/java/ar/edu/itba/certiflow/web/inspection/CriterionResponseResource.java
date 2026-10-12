package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.web.schema.CriterionPayload;
import ar.edu.itba.certiflow.web.schema.SchemaPayloads;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.time.Instant;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import tools.jackson.databind.JsonNode;

@Relation(collectionRelation = "responses", itemRelation = "response")
public class CriterionResponseResource extends RepresentationModel<CriterionResponseResource> {

    public record EvidenceView(String kind, String reference, String description, PersonPayload attachedBy, Instant attachedAt) {
    }

    public record ObservationView(String text, PersonPayload observedBy, Instant observedAt) {
    }

    public final CriterionPayload criterion;
    public final JsonNode answer;
    public final PersonPayload answeredBy;
    public final Instant answeredAt;
    public final List<EvidenceView> evidence;
    public final List<ObservationView> observations;
    public final String outcome;

    CriterionResponseResource(CriterionResponse<?> response) {
        this.criterion = SchemaPayloads.payload(response.criterion());
        this.answer = response.given().map(given -> RawAnswers.toJson(given.value().raw())).orElse(null);
        this.answeredBy = response.given().map(given -> PersonPayload.of(given.answeredBy())).orElse(null);
        this.answeredAt = response.given().map(given -> given.answeredAt()).orElse(null);
        this.evidence = response.attachments().stream()
                .map(attached -> new EvidenceView(attached.evidence().kind().label(), attached.evidence().reference(),
                        attached.evidence().description(), PersonPayload.of(attached.attachedBy()), attached.attachedAt()))
                .toList();
        this.observations = response.observations().stream()
                .map(observation -> new ObservationView(observation.text(), PersonPayload.of(observation.observedBy()),
                        observation.observedAt()))
                .toList();
        this.outcome = response.outcome().name();
    }
}
