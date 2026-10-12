package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.web.support.RequestFields;
import java.util.List;

public record SchemaRequest(String name, String assetType, List<SectionPayload> sections) {

    public SchemaRequest {
        RequestFields.require(name, "name");
        RequestFields.require(assetType, "assetType");
        sections = sections == null ? List.of() : sections;
    }
}
