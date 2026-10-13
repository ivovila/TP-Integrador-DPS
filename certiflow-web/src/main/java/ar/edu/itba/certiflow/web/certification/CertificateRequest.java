package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.web.support.RequestFields;

public record CertificateRequest(String inspectionId, String subsystemCode) {

    public CertificationScope scope() {
        return subsystemCode == null ? new CertificationScope.WholeAsset()
                : new CertificationScope.SubsystemScope(new SubsystemCode(subsystemCode));
    }

    public CertificateRequest {
        RequestFields.require(inspectionId, "inspectionId");
    }
}
