package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.shared.model.Person;

public record CancelSchemaVersionCommand(AssetType assetType, int versionNumber, String reason, Person cancelledBy) {
}
