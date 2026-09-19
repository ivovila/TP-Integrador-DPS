package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.application.finding.FindingRepository;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.exceptions.InspectionNotClosedException;
import java.util.List;

public final class NoBlockingFindingsEligibility implements CertificationEligibility {

    private final FindingRepository findings;

    public NoBlockingFindingsEligibility(FindingRepository findings) {
        this.findings = findings;
    }

    @Override
    public void ensureEligible(Inspection inspection) {
        if (!inspection.isClosed()) {
            throw new InspectionNotClosedException();
        }
        List<Finding> blocking = findings.findByAsset(inspection.asset()).stream()
                .filter(Finding::blocksCertification)
                .toList();
        if (!blocking.isEmpty()) {
            throw new BlockingFindingsPreventCertificationException(inspection.asset(), blocking.size());
        }
    }
}
