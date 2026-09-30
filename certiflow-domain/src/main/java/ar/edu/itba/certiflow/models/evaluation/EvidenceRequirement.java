package ar.edu.itba.certiflow.models.evaluation;

import java.util.List;

public record EvidenceRequirement(EvidenceKind kind, int minimum) {

    public EvidenceRequirement {
        if (minimum < 1) {
            throw new InvalidRuleConfigurationException("An evidence requirement must ask for at least one item");
        }
    }

    public boolean isSatisfiedBy(List<Evidence> attachedEvidence) {
        return attachedEvidence.stream().filter(evidence -> evidence.isOfKind(kind)).count() >= minimum;
    }
}
