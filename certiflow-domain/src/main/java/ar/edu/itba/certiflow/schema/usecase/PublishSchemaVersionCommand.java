package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;
import java.util.Set;

public record PublishSchemaVersionCommand(AssetType assetType, List<Section> sections, Person publishedBy,
                                          RequestedEffectiveDate requestedEffectiveDate,
                                          Set<SubsystemCode> requiredSubsystems) {

    public PublishSchemaVersionCommand(AssetType assetType, List<Section> sections, Person publishedBy) {
        this(assetType, sections, publishedBy, new EffectiveOnPublication(), Set.of());
    }
}
