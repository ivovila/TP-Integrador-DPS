package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.web.support.RequestFields;
import java.util.List;

public record VersionRequest(List<SectionPayload> sections) {

    public VersionRequest {
        sections = RequestFields.requireElements(sections, "sections");
    }
}
