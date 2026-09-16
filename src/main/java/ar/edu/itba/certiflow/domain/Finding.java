package ar.edu.itba.certiflow.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Finding(
        UUID id,
        UUID inspectionId,
        int revision,
        String criterionCode,
        Severity severity,
        String explanation,
        List<Evidence> evidence,
        String responsible) {
    public Finding {
        Objects.requireNonNull(id);
        Objects.requireNonNull(inspectionId);
        Checks.require(revision > 0, "Invalid revision");
        criterionCode = Checks.text(criterionCode, "criterionCode");
        Objects.requireNonNull(severity);
        explanation = Checks.text(explanation, "explanation");
        evidence = List.copyOf(evidence);
        responsible = Checks.text(responsible, "responsible");
    }
}
