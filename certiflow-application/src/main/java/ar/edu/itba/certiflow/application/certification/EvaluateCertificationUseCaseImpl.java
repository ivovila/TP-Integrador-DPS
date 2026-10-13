package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationUseCase;
import ar.edu.itba.certiflow.inspection.model.InspectionId;

public final class EvaluateCertificationUseCaseImpl implements EvaluateCertificationUseCase {

    private final InspectionResultTranslator inspectionResultTranslator;
    private final CertificationAssessmentService certificationAssessment;

    public EvaluateCertificationUseCaseImpl(InspectionResultTranslator inspectionResultTranslator,
                                            CertificationAssessmentService certificationAssessment) {
        this.inspectionResultTranslator = inspectionResultTranslator;
        this.certificationAssessment = certificationAssessment;
    }

    @Override
    public CertificationDecision execute(InspectionId inspectionId, CertificationScope scope) {
        return certificationAssessment.evaluate(inspectionResultTranslator.translate(inspectionId).forScope(scope));
    }
}
