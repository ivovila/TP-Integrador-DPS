package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class NoInspectionSchemaForAssetTypeException extends DomainException {

    public NoInspectionSchemaForAssetTypeException(AssetType assetType) {
        super("There is no inspection schema for asset type " + assetType.name());
    }
}
