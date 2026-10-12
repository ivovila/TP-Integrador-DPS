package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationEligibility;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationUseCase;

public final class EvaluateCertificationUseCaseImpl implements EvaluateCertificationUseCase {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final CertificationEligibility certificationEligibility;

    public EvaluateCertificationUseCaseImpl(InspectionRepository inspectionRepository, FindingRepository findingRepository,
                                           CertificationEligibility certificationEligibility) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.certificationEligibility = certificationEligibility;
    }

    @Override
    public CertificationDecision execute(InspectionId inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        return certificationEligibility.evaluate(inspection, findingRepository.findByAsset(inspection.assetCode()));
    }
}
