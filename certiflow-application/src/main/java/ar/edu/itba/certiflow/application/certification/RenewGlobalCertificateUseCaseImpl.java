package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateBasis;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.CertificateStatus;
import ar.edu.itba.certiflow.certification.model.CertificationConflictException;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.RenewGlobalCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewGlobalCertificateUseCase;
import java.util.Optional;

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
        ensureRenewable(currentCertificate, decided);
        certificateIssuance.ensureScopeAvailable(decided.assetCode(), decided.scope(), decided.validFrom(),
                Optional.of(currentCertificate.number()));
        Certificate renewedCertificate = certificateService.issueGlobal(certificateNumbering.next(), decided.assetCode(),
                decided.certificateBasis(), decided.schemaReference(), decided.requiredSubsystems(), decided.decision(),
                command.issuedBy(), decided.decidedAt());
        certificateService.markRenewed(currentCertificate, renewedCertificate, command.issuedBy(), decided.decidedAt());
        certificateRepository.save(currentCertificate);
        certificateRepository.save(renewedCertificate);
        return renewedCertificate.number();
    }

    private void ensureRenewable(Certificate currentCertificate, DecidedGlobalCertification decided) {
        if (!(currentCertificate.basis() instanceof CertificateBasis.PartialCertificatesBasis)
                || !currentCertificate.certifies(decided.assetCode())
                || !decided.latestInspectionClosedAt().isAfter(currentCertificate.latestInspectionClosedAt())
                || currentCertificate.statusOn(decided.validFrom(), certificateRepository.findByAsset(decided.assetCode())) == CertificateStatus.RENEWED) {
            throw new CertificationConflictException("Global renewal requires matching asset and later inspection basis");
        }
    }
}
