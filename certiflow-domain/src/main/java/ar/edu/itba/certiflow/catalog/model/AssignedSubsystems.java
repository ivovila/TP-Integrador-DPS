package ar.edu.itba.certiflow.catalog.model;

import java.util.List;

/** Los subsistemas que nombra una asociación: uno, o ninguno. */
public final class AssignedSubsystems implements SubsystemAssociationDescriber<List<SubsystemCode>> {

    @Override
    public List<SubsystemCode> noSubsystem() {
        return List.of();
    }

    @Override
    public List<SubsystemCode> inSubsystem(SubsystemCode subsystemCode) {
        return List.of(subsystemCode);
    }
}
