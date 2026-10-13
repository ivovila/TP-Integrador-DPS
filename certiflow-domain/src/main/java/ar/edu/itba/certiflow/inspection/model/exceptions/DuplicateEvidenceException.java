package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class DuplicateEvidenceException extends RuleViolationException {

    public DuplicateEvidenceException(String criterionCode, String reference) {
        super("Evidence " + reference + " is already attached to criterion " + criterionCode);
    }
}
