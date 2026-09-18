package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.shared.DomainException;

public class InspectionOfAnotherAssetException extends DomainException {

    public InspectionOfAnotherAssetException(Asset asset) {
        super("A certificate for asset " + asset.code().value() + " must be based on an inspection of that asset");
    }
}
