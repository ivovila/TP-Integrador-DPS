package ar.edu.itba.certiflow.web.schema;

import java.util.List;

public record VersionRequest(List<SectionPayload> sections) {

    public VersionRequest {
        sections = sections == null ? List.of() : sections;
    }
}
