package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;
import java.util.Set;

public record CreateInspectionSchemaCommand(String name, AssetType assetType, List<Section> sections, Person createdBy, Set<SubsystemCode> requiredSubsystems) {
    public CreateInspectionSchemaCommand(String name, AssetType assetType, List<Section> sections, Person createdBy) {
        this(name, assetType, sections, createdBy, Set.of());
    }
}
