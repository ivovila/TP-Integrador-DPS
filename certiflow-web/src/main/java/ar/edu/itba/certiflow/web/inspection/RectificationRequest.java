package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.web.support.RequestFields;
import tools.jackson.databind.JsonNode;

public record RectificationRequest(String criterionCode, JsonNode value, String reason) {

    public RectificationRequest {
        RequestFields.require(criterionCode, "criterionCode");
        RequestFields.require(reason, "reason");
    }
}
