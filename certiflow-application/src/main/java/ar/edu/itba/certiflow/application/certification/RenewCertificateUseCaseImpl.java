package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.CertificationEligibility;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import java.time.Clock;
import java.time.LocalDate;

public final class RenewCertificateUseCaseImpl implements RenewCertificateUseCase {

    private final InspectionResultTranslator inspectionResultTranslator;
    private final CertificateRepository certificateRepository;
    private final CertificationEligibility certificationEligibility;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;
    private final Clock clock;

    public RenewCertificateUseCaseImpl(InspectionResultTranslator inspectionResultTranslator, CertificateRepository certificateRepository,
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
    public CertificateNumber execute(RenewCertificateCommand command) {
        Certificate currentCertificate = certificateRepository.findByNumber(command.currentNumber())
                .orElseThrow(() -> new CertificateNotFoundException(command.currentNumber()));
        InspectionResult result = inspectionResultTranslator.translate(command.inspectionId());
        certificationEligibility.ensureEligible(result);
        ValidityPeriod renewalValidity = new ValidityPeriod(LocalDate.now(clock), command.validUntil());
        certificateService.ensureRenewableWith(currentCertificate, result);
        Certificate renewedCertificate = certificateService.renew(currentCertificate, certificateNumbering.next(),
                renewalValidity, result, command.renewedBy(), clock.instant());
        certificateRepository.save(currentCertificate);
        certificateRepository.save(renewedCertificate);
        return renewedCertificate.number();
    }
}
