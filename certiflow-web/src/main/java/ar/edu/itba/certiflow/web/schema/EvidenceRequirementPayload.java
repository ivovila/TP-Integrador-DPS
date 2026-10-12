package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.web.support.RequestFields;

public record EvidenceRequirementPayload(String kind, int minimum) {

    public EvidenceRequirementPayload {
        RequestFields.require(kind, "requiredEvidence.kind");
    }
}
