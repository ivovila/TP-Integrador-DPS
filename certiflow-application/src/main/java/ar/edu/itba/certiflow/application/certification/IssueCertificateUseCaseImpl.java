package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.CertificationEligibility;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
import ar.edu.itba.certiflow.certification.model.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import java.time.Clock;
import java.time.LocalDate;

public final class IssueCertificateUseCaseImpl implements IssueCertificateUseCase {

    private final InspectionResultTranslator inspectionResultTranslator;
    private final CertificateRepository certificateRepository;
    private final CertificationEligibility certificationEligibility;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;
    private final Clock clock;

    public IssueCertificateUseCaseImpl(InspectionResultTranslator inspectionResultTranslator, CertificateRepository certificateRepository,
                                       CertificationEligibility certificationEligibility, CertificateNumbering certificateNumbering,
                                       CertificateService certificateService, Clock clock) {
        this.inspectionResultTranslator = inspectionResultTranslator;
        this.certificateRepository = certificateRepository;
        this.certificationEligibility = certificationEligibility;
        this.certificateNumbering = certificateNumbering;
        this.certificateService = certificateService;
        this.clock = clock;
    }

    @Override
    public CertificateNumber execute(IssueCertificateCommand command) {
        InspectionResult result = inspectionResultTranslator.translate(command.inspectionId());
        LocalDate today = LocalDate.now(clock);
        certificationEligibility.ensureEligible(result);
        certificateRepository.findCurrentFor(result.assetCode(), today).ifPresent(currentCertificate -> {
            throw new AssetAlreadyCertifiedException(result.assetCode(), currentCertificate.number());
        });
        ValidityPeriod validity = new ValidityPeriod(today, command.validUntil());
        Certificate certificate = certificateService.issue(certificateNumbering.next(), result, validity, command.issuedBy(),
                clock.instant());
        certificateRepository.save(certificate);
        return certificate.number();
    }
}
