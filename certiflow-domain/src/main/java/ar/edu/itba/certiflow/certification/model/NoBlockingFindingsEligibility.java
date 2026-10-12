package ar.edu.itba.certiflow.certification.model;

import java.util.ArrayList;
import java.util.List;

public final class NoBlockingFindingsEligibility implements CertificationEligibility {

    @Override
    public CertificationDecision evaluate(InspectionResult result) {
        List<Ineligibility> ineligibilities = new ArrayList<>();
        if (!result.isClosed()) {
            ineligibilities.add(new Ineligibility(IneligibilityReason.INSPECTION_NOT_CLOSED,
                    "Inspection " + result.inspectionId().value() + " is still open"));
        }
        if (!result.coversWholeSchema()) {
            ineligibilities.add(new Ineligibility(IneligibilityReason.PARTIAL_INSPECTION,
                    "Only an inspection that covers the whole schema can back a certificate"));
        }
        int blockingFindings = result.blockingNonConformities().size();
        if (blockingFindings > 0) {
            ineligibilities.add(new Ineligibility(IneligibilityReason.BLOCKING_FINDINGS,
                    "Asset " + result.assetCode().value() + " has " + blockingFindings
                            + " open finding(s) whose severity blocks certification"));
        }
        return new CertificationDecision(ineligibilities);
    }
}
