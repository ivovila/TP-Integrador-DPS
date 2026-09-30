package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.models.certification.CertificationDecision;
import ar.edu.itba.certiflow.models.certification.CertificationEligibility;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;

public final class EvaluateCertification {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final CertificationEligibility certificationEligibility;

    public EvaluateCertification(InspectionRepository inspectionRepository, FindingRepository findingRepository,
                                 CertificationEligibility certificationEligibility) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.certificationEligibility = certificationEligibility;
    }

    public CertificationDecision execute(InspectionId inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        return certificationEligibility.evaluate(inspection, findingRepository.findByAsset(inspection.asset()));
    }
}
