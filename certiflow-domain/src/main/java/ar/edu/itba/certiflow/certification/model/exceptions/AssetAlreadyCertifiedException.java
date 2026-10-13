package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class AssetAlreadyCertifiedException extends RuleViolationException {

    public AssetAlreadyCertifiedException(AssetCode assetCode, CertificateNumber current) {
        super("Asset " + assetCode.value() + " already holds certificate " + current.value()
                + "; renew it instead of issuing another");
    }
}
