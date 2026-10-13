package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import java.util.Optional;

public final class RenewCertificateUseCaseImpl implements RenewCertificateUseCase {

    private final CertificateIssuanceService certificateIssuance;
    private final CertificateRepository certificateRepository;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;

    public RenewCertificateUseCaseImpl(CertificateIssuanceService certificateIssuance, CertificateRepository certificateRepository,
                                       CertificateNumbering certificateNumbering, CertificateService certificateService) {
        this.certificateIssuance = certificateIssuance;
        this.certificateRepository = certificateRepository;
        this.certificateNumbering = certificateNumbering;
        this.certificateService = certificateService;
    }

    @Override
    public CertificateNumber execute(RenewCertificateCommand command) {
        Certificate currentCertificate = certificateRepository.findByNumber(command.currentNumber())
                .orElseThrow(() -> new CertificateNotFoundException(command.currentNumber()));
        DecidedCertification decided = certificateIssuance.decide(command.inspectionId(), command.scope());
        certificateService.ensureRenewableWith(currentCertificate, decided.result());
        certificateIssuance.ensureScopeAvailable(decided.assetCode(), decided.scope(), decided.validFrom(),
                Optional.of(currentCertificate.number()));
        Certificate renewedCertificate = certificateService.renew(currentCertificate, certificateNumbering.next(),
                decided.decision(), decided.result(), command.renewedBy(), decided.decidedAt());
        certificateRepository.save(currentCertificate);
        certificateRepository.save(renewedCertificate);
        return renewedCertificate.number();
    }
}
