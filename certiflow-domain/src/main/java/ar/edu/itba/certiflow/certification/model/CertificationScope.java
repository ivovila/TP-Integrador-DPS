package ar.edu.itba.certiflow.certification.model;

import java.util.List;

public interface CertificationScope {

    InspectionResult narrow(InspectionResult wholeAssetResult);

    boolean requiresPartialCertificates(List<SubsystemResult> subsystems);

    <T> T describedBy(CertificationScopeDescriber<T> describer);
}
