package ar.edu.itba.certiflow.catalog.model;

public interface SubsystemAssociationDescriber<T> {

    T noSubsystem();

    T inSubsystem(SubsystemCode subsystemCode);
}
