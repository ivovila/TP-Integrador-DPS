package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.CertificationScopeDescriber;

final class ScopeTypeColumn implements CertificationScopeDescriber<String> {

    @Override
    public String wholeAsset() {
        return "WHOLE";
    }

    @Override
    public String subsystem(SubsystemCode subsystemCode) {
        return "SUBSYSTEM";
    }
}
