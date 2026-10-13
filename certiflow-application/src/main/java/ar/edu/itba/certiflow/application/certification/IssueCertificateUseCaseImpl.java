package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.DecidedCertification;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.shared.model.Stamp;

public final class IssueCertificateUseCaseImpl implements IssueCertificateUseCase {

    private final CertificateIssuanceService certificateIssuance;
    private final CertificateRepository certificateRepository;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;
    private final InspectionRepository inspectionRepository;
    private final AssetLock assetLock;

    public IssueCertificateUseCaseImpl(CertificateIssuanceService certificateIssuance, CertificateRepository certificateRepository,
                                       CertificateNumbering certificateNumbering, CertificateService certificateService,
                                       InspectionRepository inspectionRepository, AssetLock assetLock) {
        this.certificateIssuance = certificateIssuance;
        this.certificateRepository = certificateRepository;
        this.certificateNumbering = certificateNumbering;
        this.certificateService = certificateService;
        this.inspectionRepository = inspectionRepository;
        this.assetLock = assetLock;
    }

    @Override
    public CertificateNumber execute(IssueCertificateCommand command) {
        AssetCode assetCode = inspectionRepository.assetCodeOf(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        assetLock.lock(assetCode);
        DecidedCertification decided = certificateIssuance.decide(command.inspectionId(), command.scope());
        certificateIssuance.ensureScopeAvailable(decided.assetCode(), decided.scope(), decided.validFrom());
        Certificate certificate = certificateService.issue(certificateNumbering.next(), decided.result(), decided.decision(),
                new Stamp(command.issuedBy(), decided.decidedAt()));
        certificateRepository.save(certificate);
        return certificate.number();
    }
}
