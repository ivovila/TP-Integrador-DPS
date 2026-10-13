package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.inspection.model.InspectionId;

public record EvaluateCertificationCommand(InspectionId inspectionId, CertificationScope scope) {
}
