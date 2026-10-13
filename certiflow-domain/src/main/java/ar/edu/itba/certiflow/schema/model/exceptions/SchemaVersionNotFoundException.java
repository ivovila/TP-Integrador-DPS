package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.shared.model.DomainException;

public final class SchemaVersionNotFoundException extends DomainException {
    public SchemaVersionNotFoundException(AssetType assetType, int number) {
        super("Schema " + assetType.name() + " has no version " + number);
    }
}
