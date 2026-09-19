package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.application.exceptions.BlockingFindingsPreventCertificationException;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.ports.FindingRepository;
import java.util.List;

public final class NoBlockingFindingsEligibility implements CertificationEligibility {

    private final FindingRepository findingRepository;

    public NoBlockingFindingsEligibility(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    @Override
    public void ensureEligible(Inspection inspection) {
        if (!inspection.isClosed()) {
            throw new InspectionNotClosedException();
        }
        List<Finding> blockingFindings = findingRepository.findByAsset(inspection.asset()).stream()
                .filter(Finding::blocksCertification)
                .toList();
        if (!blockingFindings.isEmpty()) {
            throw new BlockingFindingsPreventCertificationException(inspection.asset(), blockingFindings.size());
        }
    }
}
