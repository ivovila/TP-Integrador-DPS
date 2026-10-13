package ar.edu.itba.certiflow.catalog.model;

public record InSubsystem(SubsystemCode subsystemCode) implements SubsystemAssociation {

    @Override
    public SubsystemAssociation orElse(SubsystemAssociation inherited) {
        return this;
    }

    @Override
    public boolean isAssignedTo(SubsystemCode candidate) {
        return subsystemCode.equals(candidate);
    }

    @Override
    public boolean reaches(SubsystemCode candidate) {
        return subsystemCode.equals(candidate);
    }

    @Override
    public <T> T describedBy(SubsystemAssociationDescriber<T> describer) {
        return describer.inSubsystem(subsystemCode);
    }
}
