package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class DuplicateEvidenceException extends DomainException {

    public DuplicateEvidenceException(String criterionCode, String reference) {
        super("Evidence " + reference + " is already attached to criterion " + criterionCode);
    }
}
