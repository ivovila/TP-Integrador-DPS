package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;
import java.util.List;

public class InspectionIncompleteException extends DomainException {

    private final List<String> pendingCriteria;

    public InspectionIncompleteException(List<String> pendingCriteria) {
        super("Criteria still pending an answer or their required evidence: " + pendingCriteria);
        this.pendingCriteria = List.copyOf(pendingCriteria);
    }

    public List<String> pendingCriteria() {
        return pendingCriteria;
    }
}
