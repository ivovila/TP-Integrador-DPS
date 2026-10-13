package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class SchemaAlreadyDefinedForAssetTypeException extends RuleViolationException {

    public SchemaAlreadyDefinedForAssetTypeException(AssetType assetType) {
        super("Asset type " + assetType.name() + " already has an inspection schema; publish a new version instead");
    }
}
