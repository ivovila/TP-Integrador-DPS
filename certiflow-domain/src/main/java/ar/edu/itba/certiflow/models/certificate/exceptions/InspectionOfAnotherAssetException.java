package ar.edu.itba.certiflow.models.certificate.exceptions;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class InspectionOfAnotherAssetException extends DomainException {

    public InspectionOfAnotherAssetException(Asset asset) {
        super("A certificate for asset " + asset.code().value() + " must be based on an inspection of that asset");
    }
}
