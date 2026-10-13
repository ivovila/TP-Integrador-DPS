package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class DuplicateEvidenceException extends RuleViolationException {

    public DuplicateEvidenceException(CriterionCode criterionCode, String reference) {
        super("Evidence " + reference + " is already attached to criterion " + criterionCode.value());
    }
}
