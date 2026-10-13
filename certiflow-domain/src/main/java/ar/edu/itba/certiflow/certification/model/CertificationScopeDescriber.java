package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;

public interface CertificationScopeDescriber<T> {

    T wholeAsset();

    T subsystem(SubsystemCode subsystemCode);
}
