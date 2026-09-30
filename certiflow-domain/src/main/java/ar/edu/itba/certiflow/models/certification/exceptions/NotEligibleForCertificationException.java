package ar.edu.itba.certiflow.models.certification.exceptions;

import ar.edu.itba.certiflow.models.certification.CertificationDecision;
import ar.edu.itba.certiflow.models.certification.Ineligibility;
import ar.edu.itba.certiflow.models.shared.DomainException;
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
