package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.domain.asset.AssetType;
import ar.edu.itba.certiflow.domain.shared.DomainException;

public class SchemaAlreadyDefinedForAssetTypeException extends DomainException {

    public SchemaAlreadyDefinedForAssetTypeException(AssetType assetType) {
        super("Asset type " + assetType.name() + " already has an inspection schema; publish a new version instead");
    }
}
