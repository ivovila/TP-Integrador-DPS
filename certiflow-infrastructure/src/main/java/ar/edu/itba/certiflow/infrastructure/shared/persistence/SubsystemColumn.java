package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.catalog.model.AssignedSubsystems;
import ar.edu.itba.certiflow.catalog.model.InSubsystem;
import ar.edu.itba.certiflow.catalog.model.NoSubsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.util.Optional;

/** La columna (o el campo del documento) del subsistema: queda vacía cuando no hay subsistema asignado. */
public record SubsystemColumn(String subsystemCode) {

    public SubsystemColumn(SubsystemAssociation association) {
        this(association.describedBy(new AssignedSubsystems()).stream()
                .findFirst()
                .map(SubsystemCode::value)
                .orElse(null));
    }

    public SubsystemAssociation association() {
        return Optional.ofNullable(subsystemCode)
                .<SubsystemAssociation>map(code -> new InSubsystem(new SubsystemCode(code)))
                .orElseGet(NoSubsystem::new);
    }
}
