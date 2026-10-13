package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.usecase.*;
import java.util.Optional;

public final class IssueGlobalCertificateUseCaseImpl implements IssueGlobalCertificateUseCase {
    private final GlobalCertificates certificates;
    public IssueGlobalCertificateUseCaseImpl(GlobalCertificates certificates) { this.certificates = certificates; }
    @Override public CertificateNumber execute(IssueGlobalCertificateCommand command) {
        return certificates.issue(command.assetCode(), command.schemaVersion(), command.partialCertificateNumbers(),
                command.issuedBy(), Optional.empty());
    }
}
