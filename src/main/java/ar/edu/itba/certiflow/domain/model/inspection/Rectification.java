package ar.edu.itba.certiflow.domain.model.inspection;

import java.time.LocalDateTime;
import java.util.Map;

import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.model.shared.Response;

public record Rectification(PersonId author, String reason, LocalDateTime rectifiedAt,
                            Map<CriterionId, Response> corrections) {

    public Rectification {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Una rectificacion requiere un motivo");
        }
        corrections = Map.copyOf(corrections);
        if (corrections.isEmpty()) {
            throw new IllegalArgumentException("Una rectificacion requiere al menos una correccion");
        }
    }
}
