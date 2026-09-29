package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.application.exceptions.BlockingFindingsPreventCertificationException;
import ar.edu.itba.certiflow.application.exceptions.UnaddressedNonConformitiesPreventCertificationException;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.models.inspection.exceptions.PartialInspectionCannotCertifyException;
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
        if (!inspection.coversWholeSchema()) {
            throw new PartialInspectionCannotCertifyException();
        }
        List<Finding> assetFindings = findingRepository.findByAsset(inspection.asset());
        List<String> unaddressedNonConformities = inspection.evaluate().nonConformities().stream()
                .filter(nonConformity -> nonConformity.criterion().severity().blocksCertification())
                .filter(nonConformity -> assetFindings.stream().noneMatch(finding -> finding.raisedFrom(inspection, nonConformity)))
                .map(nonConformity -> nonConformity.criterion().code())
                .toList();
        if (!unaddressedNonConformities.isEmpty()) {
            throw new UnaddressedNonConformitiesPreventCertificationException(inspection.asset(), unaddressedNonConformities);
        }
        List<Finding> blockingFindings = assetFindings.stream()
                .filter(Finding::blocksCertification)
                .toList();
        if (!blockingFindings.isEmpty()) {
            throw new BlockingFindingsPreventCertificationException(inspection.asset(), blockingFindings.size());
        }
    }
}
