package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.shared.DomainException;

public class BlockingFindingsPreventCertificationException extends DomainException {

    public BlockingFindingsPreventCertificationException(Asset asset, int blockingFindings) {
        super("Asset " + asset.code().value() + " has " + blockingFindings
                + " open finding(s) whose severity blocks certification");
    }
}
