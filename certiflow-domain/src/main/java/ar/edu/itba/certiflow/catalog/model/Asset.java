package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.util.List;
import java.util.Objects;

public record Asset(AssetCode code, String name, AssetType type, Location location, Person responsible,
                    Characteristics characteristics, JurisdictionCode jurisdictionCode, List<Subsystem> subsystems) {

    public Asset {
        subsystems = List.copyOf(subsystems);
        if (subsystems.stream().map(Subsystem::code).distinct().count() != subsystems.size()) {
            throw new RequiredValueMissingException("Subsystem codes must be unique within an asset");
        }
        Objects.requireNonNull(jurisdictionCode, "jurisdictionCode");
        if (name.isBlank()) {
            throw new RequiredValueMissingException("An asset needs a name");
        }
    }
}
