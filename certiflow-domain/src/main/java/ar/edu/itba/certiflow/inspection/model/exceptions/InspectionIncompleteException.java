package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;
import java.util.List;

public class InspectionIncompleteException extends RuleViolationException {

    private final List<String> pendingCriteria;

    public InspectionIncompleteException(List<String> pendingCriteria) {
        super("Criteria still pending an answer or their required evidence: " + pendingCriteria);
        this.pendingCriteria = List.copyOf(pendingCriteria);
    }

    public List<String> pendingCriteria() {
        return pendingCriteria;
    }
}
