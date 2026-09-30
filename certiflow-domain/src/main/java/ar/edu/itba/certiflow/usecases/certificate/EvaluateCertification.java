package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.models.certification.CertificationDecision;
import ar.edu.itba.certiflow.models.inspection.InspectionId;

public interface EvaluateCertification {

    CertificationDecision execute(InspectionId inspectionId);
}
