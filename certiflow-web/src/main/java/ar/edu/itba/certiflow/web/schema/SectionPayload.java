package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.web.support.RequestFields;
import java.util.List;

public record SectionPayload(String title, List<CriterionPayload> criteria, String subsystemCode) {

    public SectionPayload {
        RequestFields.require(title, "section.title");
        criteria = RequestFields.requireElements(criteria, "criteria");
    }
}
