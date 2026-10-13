package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.web.support.RequestFields;
import java.util.Optional;

public record CertificateRequest(String inspectionId, String subsystemCode) {

    public CertificationScope scope() {
        return requestedScope().orElseGet(CertificationScope.WholeAsset::new);
    }

    /** Al renovar, sin subsistema se conserva el alcance del certificado actual. */
    public Optional<CertificationScope> requestedScope() {
        return Optional.ofNullable(subsystemCode).map(code -> new CertificationScope.SubsystemScope(new SubsystemCode(code)));
    }

    public CertificateRequest {
        RequestFields.require(inspectionId, "inspectionId");
    }
}
