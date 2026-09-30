package ar.edu.itba.certiflow.models.schema.exceptions;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class NoInspectionSchemaForAssetTypeException extends DomainException {

    public NoInspectionSchemaForAssetTypeException(AssetType assetType) {
        super("There is no inspection schema for asset type " + assetType.name());
    }
}
