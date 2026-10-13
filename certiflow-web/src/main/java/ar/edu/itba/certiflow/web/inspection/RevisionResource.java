package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.model.Revision;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "revisions", itemRelation = "revision")
public class RevisionResource extends RepresentationModel<RevisionResource> {

    public final int number;
    public final String reason;
    public final PersonPayload sealedBy;
    public final Instant sealedAt;
    public final Map<String, String> outcomes = new LinkedHashMap<>();

    RevisionResource(Revision revision) {
        this.number = revision.number();
        this.reason = revision.reason();
        this.sealedBy = PersonPayload.of(revision.sealedBy());
        this.sealedAt = revision.sealedAt();
        revision.evaluation().responses().forEach(response -> outcomes.put(response.criterion().code().value(), response.outcome().name()));
    }
}
