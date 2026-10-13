package ar.edu.itba.certiflow.certification.model;

import java.util.List;

public record Requirements<T>(List<Requirement<T>> requirements) {

    public Requirements {
        requirements = List.copyOf(requirements);
    }

    /** Un motivo por cada regla que el candidato no cumple, en el orden en que se declararon. */
    public List<Ineligibility> unmetBy(T candidate) {
        return requirements.stream()
                .filter(requirement -> !requirement.isMetBy(candidate))
                .map(requirement -> requirement.ineligibilityOf(candidate))
                .toList();
    }

    public boolean areMetBy(T candidate) {
        return requirements.stream().allMatch(requirement -> requirement.isMetBy(candidate));
    }
}
