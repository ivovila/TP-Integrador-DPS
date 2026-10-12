package ar.edu.itba.certiflow.models.certificate.exceptions;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class InspectionOfAnotherAssetException extends DomainException {

    public InspectionOfAnotherAssetException(AssetCode assetCode) {
        super("A certificate for asset " + assetCode.value() + " must be based on an inspection of that asset");
    }
}
