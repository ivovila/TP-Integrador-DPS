package ar.edu.itba.certiflow.models.certification;

import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import java.util.ArrayList;
import java.util.List;

public final class NoBlockingFindingsEligibility implements CertificationEligibility {

    @Override
    public CertificationDecision evaluate(InspectionView inspection, List<Finding> assetFindings) {
        List<Ineligibility> ineligibilities = new ArrayList<>();
        if (!inspection.isClosed()) {
            ineligibilities.add(new Ineligibility(IneligibilityReason.INSPECTION_NOT_CLOSED,
                    "Inspection " + inspection.id().value() + " is still open"));
        }
        if (!inspection.coversWholeSchema()) {
            ineligibilities.add(new Ineligibility(IneligibilityReason.PARTIAL_INSPECTION,
                    "Only an inspection that covers the whole schema can back a certificate"));
        }
        long blockingFindings = assetFindings.stream().filter(Finding::blocksCertification).count();
        if (blockingFindings > 0) {
            ineligibilities.add(new Ineligibility(IneligibilityReason.BLOCKING_FINDINGS,
                    "Asset " + inspection.assetCode().value() + " has " + blockingFindings
                            + " open finding(s) whose severity blocks certification"));
        }
        return new CertificationDecision(ineligibilities);
    }
}
