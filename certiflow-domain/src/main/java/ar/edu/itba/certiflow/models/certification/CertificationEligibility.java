package ar.edu.itba.certiflow.models.certification;

import ar.edu.itba.certiflow.models.certification.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import java.util.List;

public interface CertificationEligibility {

    CertificationDecision evaluate(InspectionView inspection, List<Finding> assetFindings);

    /** Emitir y renovar exigen lo mismo: la decisión se consulta una vez y, si no es elegible, viaja en la excepción. */
    default void ensureEligible(InspectionView inspection, List<Finding> assetFindings) {
        CertificationDecision decision = evaluate(inspection, assetFindings);
        if (!decision.isEligible()) {
            throw new NotEligibleForCertificationException(decision);
        }
    }
}
