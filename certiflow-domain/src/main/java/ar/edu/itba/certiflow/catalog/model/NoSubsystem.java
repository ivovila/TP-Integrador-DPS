package ar.edu.itba.certiflow.catalog.model;

/** Sin subsistema propio: hereda el que le den, no está asignado a ninguno y alcanza a todos. */
public record NoSubsystem() implements SubsystemAssociation {

    @Override
    public SubsystemAssociation orElse(SubsystemAssociation inherited) {
        return inherited;
    }

    @Override
    public boolean isAssignedTo(SubsystemCode subsystemCode) {
        return false;
    }

    @Override
    public boolean reaches(SubsystemCode subsystemCode) {
        return true;
    }

    @Override
    public <T> T describedBy(SubsystemAssociationDescriber<T> describer) {
        return describer.noSubsystem();
    }
}
