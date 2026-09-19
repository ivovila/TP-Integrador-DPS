package ar.edu.itba.certiflow.application.exceptions;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class BlockingFindingsPreventCertificationException extends DomainException {

    public BlockingFindingsPreventCertificationException(Asset asset, int blockingFindings) {
        super("Asset " + asset.code().value() + " has " + blockingFindings
                + " open finding(s) whose severity blocks certification");
    }
}
