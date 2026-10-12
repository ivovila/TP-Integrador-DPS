package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;

public interface CertificationEligibility {

    CertificationDecision evaluate(InspectionResult result);

    /** Emitir y renovar exigen lo mismo: la decisión se consulta una vez y, si no es elegible, viaja en la excepción. */
    default void ensureEligible(InspectionResult result) {
        CertificationDecision decision = evaluate(result);
        if (!decision.isEligible()) {
            throw new NotEligibleForCertificationException(decision);
        }
    }
}
