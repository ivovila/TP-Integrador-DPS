package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.IssueGlobalCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueGlobalCertificateUseCase;
import java.util.Optional;

public final class IssueGlobalCertificateUseCaseImpl implements IssueGlobalCertificateUseCase {

    private final GlobalCertificationService globalCertification;
    private final CertificateIssuanceService certificateIssuance;
    private final CertificateRepository certificateRepository;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;

    public IssueGlobalCertificateUseCaseImpl(GlobalCertificationService globalCertification,
                                             CertificateIssuanceService certificateIssuance,
                                             CertificateRepository certificateRepository,
                                             CertificateNumbering certificateNumbering,
                                             CertificateService certificateService) {
        this.globalCertification = globalCertification;
        this.certificateIssuance = certificateIssuance;
        this.certificateRepository = certificateRepository;
        this.certificateNumbering = certificateNumbering;
        this.certificateService = certificateService;
    }

    @Override
    public CertificateNumber execute(IssueGlobalCertificateCommand command) {
        DecidedGlobalCertification decided = globalCertification.decide(command.assetCode(), command.schemaVersion(),
                command.partialCertificateNumbers());
        certificateIssuance.ensureScopeAvailable(decided.assetCode(), decided.scope(), decided.validFrom(), Optional.empty());
        Certificate certificate = certificateService.issueGlobal(certificateNumbering.next(), decided.assetCode(),
                decided.certificateBasis(), decided.schemaReference(), decided.requiredSubsystems(), decided.decision(),
                command.issuedBy(), decided.decidedAt());
        certificateRepository.save(certificate);
        return certificate.number();
    }
}
