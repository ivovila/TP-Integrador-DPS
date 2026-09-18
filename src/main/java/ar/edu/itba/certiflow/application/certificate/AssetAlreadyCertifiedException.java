package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.certificate.CertificateNumber;
import ar.edu.itba.certiflow.domain.shared.DomainException;

public class AssetAlreadyCertifiedException extends DomainException {

    public AssetAlreadyCertifiedException(Asset asset, CertificateNumber current) {
        super("Asset " + asset.code().value() + " already holds certificate " + current.value()
                + "; renew it instead of issuing another");
    }
}
