package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.web.schema.SchemaPayloads;
import ar.edu.itba.certiflow.web.support.RequestFields;

public record EvidenceRequest(String kind, String reference, String description) {

    public EvidenceRequest {
        RequestFields.require(kind, "kind");
        RequestFields.require(reference, "reference");
        description = description == null ? "" : description;
    }

    Evidence toDomain() {
        return new Evidence(SchemaPayloads.evidenceKind(kind), reference, description);
    }
}
