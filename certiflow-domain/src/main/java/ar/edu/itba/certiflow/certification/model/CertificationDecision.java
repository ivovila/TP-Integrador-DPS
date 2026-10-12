package ar.edu.itba.certiflow.certification.model;

import java.util.List;

public record CertificationDecision(List<Ineligibility> ineligibilities) {

    public CertificationDecision {
        ineligibilities = List.copyOf(ineligibilities);
    }

    public boolean isEligible() {
        return ineligibilities.isEmpty();
    }

    public boolean isBlockedBy(IneligibilityReason reason) {
        return ineligibilities.stream().anyMatch(ineligibility -> ineligibility.reason() == reason);
    }
}
