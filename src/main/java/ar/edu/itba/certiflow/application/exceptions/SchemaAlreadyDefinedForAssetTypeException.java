package ar.edu.itba.certiflow.application.exceptions;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class SchemaAlreadyDefinedForAssetTypeException extends DomainException {

    public SchemaAlreadyDefinedForAssetTypeException(AssetType assetType) {
        super("Asset type " + assetType.name() + " already has an inspection schema; publish a new version instead");
    }
}
