package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public final class SubsystemNotFoundException extends NotFoundException {
    public SubsystemNotFoundException(SubsystemCode code) { super("Subsystem not found: " + code.value()); }
}
