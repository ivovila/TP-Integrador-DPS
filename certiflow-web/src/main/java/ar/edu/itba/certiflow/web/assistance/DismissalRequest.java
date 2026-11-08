package ar.edu.itba.certiflow.web.assistance;

import ar.edu.itba.certiflow.web.support.RequestFields;

public record DismissalRequest(String reason) {
    public DismissalRequest {
        RequestFields.require(reason, "reason");
    }
}
