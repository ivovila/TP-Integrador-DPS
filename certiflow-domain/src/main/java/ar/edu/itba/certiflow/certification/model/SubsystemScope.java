package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.util.List;

public record SubsystemScope(SubsystemCode subsystemCode) implements CertificationScope {

    @Override
    public InspectionResult narrow(InspectionResult wholeAssetResult) {
        return wholeAssetResult.ofSubsystem(subsystemCode, this);
    }

    @Override
    public boolean requiresPartialCertificates(List<SubsystemResult> subsystems) {
        return false;
    }

    @Override
    public <T> T describedBy(CertificationScopeDescriber<T> describer) {
        return describer.subsystem(subsystemCode);
    }
}
