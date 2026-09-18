package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.LocalDate;
import java.util.Objects;

/** La asignación del enunciado como una unidad: inspector designado, fecha prevista y alcance. */
public record InspectionAssignment(Person inspector, LocalDate plannedDate, String scope) {

    public InspectionAssignment {
        Objects.requireNonNull(inspector, "inspector");
        Objects.requireNonNull(plannedDate, "plannedDate");
        Objects.requireNonNull(scope, "scope");
        if (scope.isBlank()) {
            throw new IllegalArgumentException("An inspection assignment must state its scope");
        }
    }
}
