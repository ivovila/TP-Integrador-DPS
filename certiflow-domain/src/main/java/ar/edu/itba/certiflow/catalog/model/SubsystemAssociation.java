package ar.edu.itba.certiflow.catalog.model;

/** A qué subsistema pertenece algo, si pertenece a alguno. */
public interface SubsystemAssociation {

    SubsystemAssociation orElse(SubsystemAssociation inherited);

    boolean isAssignedTo(SubsystemCode subsystemCode);

    boolean reaches(SubsystemCode subsystemCode);

    <T> T describedBy(SubsystemAssociationDescriber<T> describer);
}
