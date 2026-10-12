package ar.edu.itba.certiflow.catalog.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Characteristics;
import ar.edu.itba.certiflow.catalog.model.Location;
import ar.edu.itba.certiflow.shared.model.Person;

public record RegisterAssetCommand(AssetCode code, String name, AssetType type, Location location, Person responsible, Characteristics characteristics, Person registeredBy) {
}
