package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.certification.model.RejectedDecision;
import ar.edu.itba.certiflow.certification.model.Ineligibility;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;
import java.util.stream.Collectors;

public class NotEligibleForCertificationException extends RuleViolationException {

    private final RejectedDecision decision;

    public NotEligibleForCertificationException(RejectedDecision decision) {
        super("Not eligible for certification: " + decision.ineligibilities().stream()
                .map(Ineligibility::detail)
                .collect(Collectors.joining("; ")));
        this.decision = decision;
    }

    public RejectedDecision decision() {
        return decision;
    }
}
