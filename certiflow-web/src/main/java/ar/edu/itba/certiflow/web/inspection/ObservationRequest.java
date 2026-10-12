package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.web.support.RequestFields;

public record ObservationRequest(String text) {

    public ObservationRequest {
        RequestFields.require(text, "text");
    }
}
