package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class ReplacementForAnotherAssetException extends RuleViolationException {

    public ReplacementForAnotherAssetException(AssetCode assetCode) {
        super("A certificate for asset " + assetCode.value() + " can only be replaced by one for that same asset");
    }
}
