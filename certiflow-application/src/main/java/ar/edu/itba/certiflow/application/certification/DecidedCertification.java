package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Una inspección ya traducida al alcance que se quiere certificar, junto con la decisión que resultó de ella.
 * Consultar, emitir y renovar parten de esto, así que ninguno vuelve a evaluar ni recibe una decisión del cliente.
 */
public record DecidedCertification(InspectionResult result, CertificationDecision decision) {

    public AssetCode assetCode() {
        return result.assetCode();
    }

    public CertificationScope scope() {
        return result.scope();
    }

    public LocalDate validFrom() {
        return decision.validity().orElseThrow().validFrom();
    }

    public Instant decidedAt() {
        return decision.evaluatedAt();
    }
}
