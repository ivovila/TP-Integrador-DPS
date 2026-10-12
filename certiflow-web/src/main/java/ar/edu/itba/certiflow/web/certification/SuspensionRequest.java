package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.web.support.RequestFields;

public record SuspensionRequest(String reason) {

    public SuspensionRequest {
        RequestFields.require(reason, "reason");
    }
}
