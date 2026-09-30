package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class DuplicateEvidenceException extends DomainException {

    public DuplicateEvidenceException(String criterionCode, String reference) {
        super("Evidence " + reference + " is already attached to criterion " + criterionCode);
    }
}
