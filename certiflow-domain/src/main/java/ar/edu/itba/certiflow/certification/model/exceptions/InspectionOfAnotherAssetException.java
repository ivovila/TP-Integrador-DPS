package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class InspectionOfAnotherAssetException extends DomainException {

    public InspectionOfAnotherAssetException(AssetCode assetCode) {
        super("A certificate for asset " + assetCode.value() + " must be based on an inspection of that asset");
    }
}
