package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.domain.asset.AssetType;
import ar.edu.itba.certiflow.domain.shared.DomainException;

public class NoInspectionSchemaForAssetTypeException extends DomainException {

    public NoInspectionSchemaForAssetTypeException(AssetType assetType) {
        super("There is no inspection schema for asset type " + assetType.name());
    }
}
