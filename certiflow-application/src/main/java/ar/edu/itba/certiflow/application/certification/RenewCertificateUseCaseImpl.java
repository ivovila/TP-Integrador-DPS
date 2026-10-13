package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.DecidedCertification;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import ar.edu.itba.certiflow.shared.model.Stamp;

public final class RenewCertificateUseCaseImpl implements RenewCertificateUseCase {

    private final CertificateIssuanceService certificateIssuance;
    private final CertificateRepository certificateRepository;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;
    private final AssetLock assetLock;

    public RenewCertificateUseCaseImpl(CertificateIssuanceService certificateIssuance, CertificateRepository certificateRepository,
                                       CertificateNumbering certificateNumbering, CertificateService certificateService,
                                       AssetLock assetLock) {
        this.certificateIssuance = certificateIssuance;
        this.certificateRepository = certificateRepository;
        this.certificateNumbering = certificateNumbering;
        this.certificateService = certificateService;
        this.assetLock = assetLock;
    }

    @Override
    public CertificateNumber execute(RenewCertificateCommand command) {
        AssetCode assetCode = certificateRepository.assetCodeOf(command.currentNumber())
                .orElseThrow(() -> new CertificateNotFoundException(command.currentNumber()));
        assetLock.lock(assetCode);
        Certificate currentCertificate = certificateRepository.findByNumber(command.currentNumber())
                .orElseThrow(() -> new CertificateNotFoundException(command.currentNumber()));
        CertificationScope scope = command.requestedScope().resolveAgainst(currentCertificate.scope());
        DecidedCertification decided = certificateIssuance.decide(command.inspectionId(), scope);
        certificateService.ensureRenewableWith(currentCertificate, decided.result());
        certificateIssuance.ensureScopeAvailableToReplace(currentCertificate, decided.scope(), decided.validFrom());
        Certificate renewedCertificate = certificateService.renew(currentCertificate, certificateNumbering.next(),
                decided.decision(), decided.result(), new Stamp(command.renewedBy(), decided.decidedAt()));
        certificateRepository.save(currentCertificate);
        certificateRepository.save(renewedCertificate);
        return renewedCertificate.number();
    }
}
