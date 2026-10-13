package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public class NoInspectionSchemaForAssetTypeException extends NotFoundException {

    public NoInspectionSchemaForAssetTypeException(AssetType assetType) {
        super("There is no inspection schema for asset type " + assetType.name());
    }
}
