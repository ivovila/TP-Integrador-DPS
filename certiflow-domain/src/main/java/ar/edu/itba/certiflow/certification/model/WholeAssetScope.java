package ar.edu.itba.certiflow.certification.model;

import java.util.List;

public record WholeAssetScope() implements CertificationScope {

    @Override
    public InspectionResult narrow(InspectionResult wholeAssetResult) {
        return wholeAssetResult;
    }

    @Override
    public boolean requiresPartialCertificates(List<SubsystemResult> subsystems) {
        return !subsystems.isEmpty();
    }

    @Override
    public <T> T describedBy(CertificationScopeDescriber<T> describer) {
        return describer.wholeAsset();
    }
}
