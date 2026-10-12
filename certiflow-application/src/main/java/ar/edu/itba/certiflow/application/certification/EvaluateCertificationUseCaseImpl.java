package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationEligibility;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationUseCase;
import ar.edu.itba.certiflow.inspection.model.InspectionId;

public final class EvaluateCertificationUseCaseImpl implements EvaluateCertificationUseCase {

    private final InspectionResultTranslator inspectionResultTranslator;
    private final CertificationEligibility certificationEligibility;

    public EvaluateCertificationUseCaseImpl(InspectionResultTranslator inspectionResultTranslator,
                                            CertificationEligibility certificationEligibility) {
        this.inspectionResultTranslator = inspectionResultTranslator;
        this.certificationEligibility = certificationEligibility;
    }

    @Override
    public CertificationDecision execute(InspectionId inspectionId) {
        return certificationEligibility.evaluate(inspectionResultTranslator.translate(inspectionId));
    }
}
