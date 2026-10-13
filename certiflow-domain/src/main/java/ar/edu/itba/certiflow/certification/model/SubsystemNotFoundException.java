package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.shared.model.DomainException;

public final class SubsystemNotFoundException extends DomainException {
    public SubsystemNotFoundException(SubsystemCode code) { super("Subsystem not found: " + code.value()); }
}
