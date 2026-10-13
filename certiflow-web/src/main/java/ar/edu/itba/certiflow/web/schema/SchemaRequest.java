package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.web.support.RequestFields;
import java.util.List;
import java.util.Set;

public record SchemaRequest(String name, String assetType, List<SectionPayload> sections, Set<String> requiredSubsystems) {

    public SchemaRequest {
        requiredSubsystems = requiredSubsystems == null ? Set.of() : Set.copyOf(requiredSubsystems);
        RequestFields.require(name, "name");
        RequestFields.require(assetType, "assetType");
        sections = RequestFields.requireElements(sections, "sections");
    }
}
