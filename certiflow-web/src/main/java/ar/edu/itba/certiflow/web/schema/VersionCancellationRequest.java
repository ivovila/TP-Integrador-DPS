package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.web.support.RequestFields;

public record VersionCancellationRequest(String reason) {

    public VersionCancellationRequest {
        RequestFields.require(reason, "reason");
    }
}
