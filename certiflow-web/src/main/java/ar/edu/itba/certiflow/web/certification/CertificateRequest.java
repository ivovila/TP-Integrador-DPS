package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.SubsystemScope;
import ar.edu.itba.certiflow.certification.model.WholeAssetScope;
import ar.edu.itba.certiflow.certification.usecase.ExplicitRenewalScope;
import ar.edu.itba.certiflow.certification.usecase.RequestedRenewalScope;
import ar.edu.itba.certiflow.certification.usecase.SameScopeAsCurrent;
import ar.edu.itba.certiflow.web.support.RequestFields;
import java.util.Optional;

public record CertificateRequest(String inspectionId, String subsystemCode) {

    public CertificationScope scope() {
        return requestedSubsystemScope().orElseGet(WholeAssetScope::new);
    }

    /** Al renovar, sin subsistema se conserva el alcance del certificado actual. */
    public RequestedRenewalScope requestedRenewalScope() {
        return requestedSubsystemScope().<RequestedRenewalScope>map(ExplicitRenewalScope::new).orElseGet(SameScopeAsCurrent::new);
    }

    private Optional<CertificationScope> requestedSubsystemScope() {
        return Optional.ofNullable(subsystemCode).map(code -> new SubsystemScope(new SubsystemCode(code)));
    }

    public CertificateRequest {
        RequestFields.require(inspectionId, "inspectionId");
    }
}
