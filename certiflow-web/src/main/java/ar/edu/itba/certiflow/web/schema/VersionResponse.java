package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "versions", itemRelation = "version")
public class VersionResponse extends RepresentationModel<VersionResponse> {

    public final int number;
    public final Instant publishedAt;
    public final Instant effectiveFrom;
    public final Set<String> requiredSubsystems;
    public Instant resolvedAt;
    public final List<SectionPayload> sections;

    VersionResponse(SchemaVersion version) {
        this.number = version.number();
        this.publishedAt = version.publishedAt();
        this.effectiveFrom = version.effectiveFrom();
        this.requiredSubsystems = version.requiredSubsystems().stream().map(code -> code.value()).collect(Collectors.toSet());
        this.sections = SchemaPayloads.fromSections(version.sections().all());
    }
}
