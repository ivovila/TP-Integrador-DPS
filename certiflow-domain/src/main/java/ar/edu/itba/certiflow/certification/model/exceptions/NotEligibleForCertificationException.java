package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.Ineligibility;
import ar.edu.itba.certiflow.shared.model.DomainException;
import java.util.stream.Collectors;

public class NotEligibleForCertificationException extends DomainException {

    private final CertificationDecision decision;

    public NotEligibleForCertificationException(CertificationDecision decision) {
        super("Not eligible for certification: " + decision.ineligibilities().stream()
                .map(Ineligibility::detail)
                .collect(Collectors.joining("; ")));
        this.decision = decision;
    }

    public CertificationDecision decision() {
        return decision;
    }
}
