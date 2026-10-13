package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Sin fecha de vigencia pedida, la versión rige desde que se publica. */
public record PublishSchemaVersionCommand(AssetType assetType, List<Section> sections, Person publishedBy,
                                          Instant requestedEffectiveFrom, Set<SubsystemCode> requiredSubsystems) {

    public PublishSchemaVersionCommand(AssetType assetType, List<Section> sections, Person publishedBy) {
        this(assetType, sections, publishedBy, null, Set.of());
    }

    public Optional<Instant> effectiveFrom() {
        return Optional.ofNullable(requestedEffectiveFrom);
    }
}
