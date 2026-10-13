package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.web.support.RequestFields;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record VersionRequest(List<SectionPayload> sections, Instant effectiveFrom, Set<String> requiredSubsystems) {

    public VersionRequest {
        requiredSubsystems = requiredSubsystems == null ? Set.of() : Set.copyOf(requiredSubsystems);
        sections = RequestFields.requireElements(sections, "sections");
    }
}
