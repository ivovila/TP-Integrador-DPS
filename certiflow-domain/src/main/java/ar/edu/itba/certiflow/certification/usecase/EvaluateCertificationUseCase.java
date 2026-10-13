package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.inspection.model.InspectionId;

public interface EvaluateCertificationUseCase {

    CertificationDecision execute(InspectionId inspectionId, CertificationScope scope);
}
