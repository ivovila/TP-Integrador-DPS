package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.DecidedGlobalCertification;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.RenewGlobalCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewGlobalCertificateUseCase;
import ar.edu.itba.certiflow.shared.model.Stamp;

public final class RenewGlobalCertificateUseCaseImpl implements RenewGlobalCertificateUseCase {

    private final GlobalCertificationService globalCertification;
    private final CertificateIssuanceService certificateIssuance;
    private final CertificateRepository certificateRepository;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;

    public RenewGlobalCertificateUseCaseImpl(GlobalCertificationService globalCertification,
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
    public CertificateNumber execute(RenewGlobalCertificateCommand command) {
        Certificate currentCertificate = certificateRepository.findByNumber(command.currentNumber())
                .orElseThrow(() -> new CertificateNotFoundException(command.currentNumber()));
        DecidedGlobalCertification decided = globalCertification.decide(command.assetCode(), command.schemaVersion(),
                command.partialCertificateNumbers());
        certificateService.ensureGlobalRenewable(currentCertificate, decided);
        certificateIssuance.ensureScopeAvailableToReplace(currentCertificate, decided.scope(), decided.validFrom());
        Certificate renewedCertificate = certificateService.issueGlobal(certificateNumbering.next(), decided, command.issuedBy());
        certificateService.markRenewed(currentCertificate, renewedCertificate, new Stamp(command.issuedBy(), decided.decidedAt()));
        certificateRepository.save(currentCertificate);
        certificateRepository.save(renewedCertificate);
        return renewedCertificate.number();
    }
}
