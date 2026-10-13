package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;
import java.util.List;

public class InspectionIncompleteException extends RuleViolationException {

    private final List<CriterionCode> pendingCriteria;

    public InspectionIncompleteException(List<CriterionCode> pendingCriteria) {
        super("Criteria still pending an answer or their required evidence: "
                + pendingCriteria.stream().map(CriterionCode::value).toList());
        this.pendingCriteria = List.copyOf(pendingCriteria);
    }

    public List<CriterionCode> pendingCriteria() {
        return pendingCriteria;
    }
}
