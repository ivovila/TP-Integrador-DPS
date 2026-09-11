package ar.edu.itba.certiflow.domain.schema;

import java.util.Set;

public record Criterion(CriterionId id, String description, Severity severity,
                        Set<EvidenceType> requiredEvidence) {

    public Criterion {
        requiredEvidence = Set.copyOf(requiredEvidence);
    }
}
