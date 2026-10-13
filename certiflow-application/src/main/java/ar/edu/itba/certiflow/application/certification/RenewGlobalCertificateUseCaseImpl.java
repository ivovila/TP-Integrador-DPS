package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.usecase.*;
import java.util.Optional;

public final class RenewGlobalCertificateUseCaseImpl implements RenewGlobalCertificateUseCase {
    private final GlobalCertificates certificates;
    public RenewGlobalCertificateUseCaseImpl(GlobalCertificates certificates) { this.certificates = certificates; }
    @Override public CertificateNumber execute(RenewGlobalCertificateCommand command) {
        return certificates.issue(command.assetCode(), command.schemaVersion(), command.partialCertificateNumbers(),
                command.issuedBy(), Optional.of(command.currentNumber()));
    }
}
