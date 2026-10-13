package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.CertificationScopeDescriber;
import ar.edu.itba.certiflow.web.certification.CertificationPayloads.ScopePayload;

final class ScopePayloadDescriber implements CertificationScopeDescriber<ScopePayload> {

    @Override
    public ScopePayload wholeAsset() {
        return new ScopePayload("WHOLE_ASSET", null);
    }

    @Override
    public ScopePayload subsystem(SubsystemCode subsystemCode) {
        return new ScopePayload("SUBSYSTEM", subsystemCode.value());
    }
}
