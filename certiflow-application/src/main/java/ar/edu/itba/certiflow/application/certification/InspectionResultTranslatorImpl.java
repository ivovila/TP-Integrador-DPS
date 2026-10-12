package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.OpenNonConformity;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import java.util.Optional;

public final class InspectionResultTranslatorImpl implements InspectionResultTranslator {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;

    public InspectionResultTranslatorImpl(InspectionRepository inspectionRepository, FindingRepository findingRepository) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
    }

    @Override
    public InspectionResult translate(InspectionId inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        return new InspectionResult(inspection.id(), inspection.assetCode(),
                inspection.isClosed() ? Optional.of(inspection.originalRevision().sealedAt()) : Optional.empty(),
                inspection.coversWholeSchema(),
                findingRepository.findByAsset(inspection.assetCode()).stream()
                        .filter(Finding::isOpen)
                        .map(finding -> new OpenNonConformity(finding.criterion().code(), finding.severity()))
                        .toList());
    }
}
