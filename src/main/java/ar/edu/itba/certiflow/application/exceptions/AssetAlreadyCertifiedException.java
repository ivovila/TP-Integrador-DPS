package ar.edu.itba.certiflow.application.exceptions;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class AssetAlreadyCertifiedException extends DomainException {

    public AssetAlreadyCertifiedException(Asset asset, CertificateNumber current) {
        super("Asset " + asset.code().value() + " already holds certificate " + current.value()
                + "; renew it instead of issuing another");
    }
}
