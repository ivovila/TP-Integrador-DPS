package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;

/** Cómo se nombra un alcance en un texto para personas, como el detalle de la bitácora. */
final class ScopeLabel implements CertificationScopeDescriber<String> {

    @Override
    public String wholeAsset() {
        return "whole asset";
    }

    @Override
    public String subsystem(SubsystemCode subsystemCode) {
        return "subsystem " + subsystemCode.value();
    }
}
