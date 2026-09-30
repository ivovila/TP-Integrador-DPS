package ar.edu.itba.certiflow.models.certification;

import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import java.util.List;

public interface CertificationEligibility {

    CertificationDecision evaluate(InspectionView inspection, List<Finding> assetFindings);
}
