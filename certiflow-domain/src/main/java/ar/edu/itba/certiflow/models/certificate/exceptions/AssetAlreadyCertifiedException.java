package ar.edu.itba.certiflow.models.certificate.exceptions;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class AssetAlreadyCertifiedException extends DomainException {

    public AssetAlreadyCertifiedException(AssetCode assetCode, CertificateNumber current) {
        super("Asset " + assetCode.value() + " already holds certificate " + current.value()
                + "; renew it instead of issuing another");
    }
}
