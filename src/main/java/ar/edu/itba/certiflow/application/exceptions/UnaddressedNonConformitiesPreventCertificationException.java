package ar.edu.itba.certiflow.application.exceptions;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.shared.DomainException;
import java.util.List;

public class UnaddressedNonConformitiesPreventCertificationException extends DomainException {

    public UnaddressedNonConformitiesPreventCertificationException(Asset asset, List<String> criterionCodes) {
        super("Asset " + asset.code().value() + " has blocking non-conformities without a raised finding: "
                + String.join(", ", criterionCodes));
    }
}
