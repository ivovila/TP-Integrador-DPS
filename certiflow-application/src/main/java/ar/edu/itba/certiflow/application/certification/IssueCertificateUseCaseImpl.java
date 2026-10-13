package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.DecidedCertification;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import ar.edu.itba.certiflow.shared.model.Stamp;

public final class IssueCertificateUseCaseImpl implements IssueCertificateUseCase {

    private final CertificateIssuanceService certificateIssuance;
    private final CertificateRepository certificateRepository;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;

    public IssueCertificateUseCaseImpl(CertificateIssuanceService certificateIssuance, CertificateRepository certificateRepository,
                                       CertificateNumbering certificateNumbering, CertificateService certificateService) {
        this.certificateIssuance = certificateIssuance;
        this.certificateRepository = certificateRepository;
        this.certificateNumbering = certificateNumbering;
        this.certificateService = certificateService;
    }

    @Override
    public CertificateNumber execute(IssueCertificateCommand command) {
        DecidedCertification decided = certificateIssuance.decide(command.inspectionId(), command.scope());
        certificateIssuance.ensureScopeAvailable(decided.assetCode(), decided.scope(), decided.validFrom());
        Certificate certificate = certificateService.issue(certificateNumbering.next(), decided.result(), decided.decision(),
                new Stamp(command.issuedBy(), decided.decidedAt()));
        certificateRepository.save(certificate);
        return certificate.number();
    }
}
