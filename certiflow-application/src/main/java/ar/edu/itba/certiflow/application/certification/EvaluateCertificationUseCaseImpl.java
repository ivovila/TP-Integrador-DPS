package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationCommand;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationUseCase;

public final class EvaluateCertificationUseCaseImpl implements EvaluateCertificationUseCase {

    private final CertificateIssuanceService certificateIssuance;

    public EvaluateCertificationUseCaseImpl(CertificateIssuanceService certificateIssuance) {
        this.certificateIssuance = certificateIssuance;
    }

    @Override
    public CertificationDecision execute(EvaluateCertificationCommand command) {
        return certificateIssuance.evaluate(command.inspectionId(), command.scope());
    }
}
