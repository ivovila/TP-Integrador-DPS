package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.LocalDate;
import java.util.Objects;

public record InspectionAssignment(Person inspector, LocalDate plannedDate, String scope) {

    public InspectionAssignment {
        if (scope.isBlank()) {
            throw new IllegalArgumentException("An inspection assignment must state its scope");
        }
    }
}
