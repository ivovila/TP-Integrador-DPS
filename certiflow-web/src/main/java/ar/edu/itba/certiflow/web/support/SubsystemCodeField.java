package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.catalog.model.AssignedSubsystems;
import ar.edu.itba.certiflow.catalog.model.InSubsystem;
import ar.edu.itba.certiflow.catalog.model.NoSubsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.util.Optional;

/** El campo `subsystemCode` del JSON: viaja vacío cuando no hay subsistema asignado. */
public record SubsystemCodeField(String subsystemCode) {

    public SubsystemCodeField(SubsystemAssociation association) {
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
