package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;

public interface EvaluateCertificationUseCase {

    CertificationDecision execute(EvaluateCertificationCommand command);
}
