package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import java.time.Clock;

public final class RenewCertificateUseCaseImpl implements RenewCertificateUseCase {

    private final InspectionResultTranslator inspectionResultTranslator;
    private final CertificateRepository certificateRepository;
    private final CertificationAssessmentService certificationAssessment;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;
    private final Clock clock;

    public RenewCertificateUseCaseImpl(InspectionResultTranslator inspectionResultTranslator, CertificateRepository certificateRepository,
                                       CertificationAssessmentService certificationAssessment, CertificateNumbering certificateNumbering,
                                       CertificateService certificateService, Clock clock) {
        this.inspectionResultTranslator = inspectionResultTranslator;
        this.certificateRepository = certificateRepository;
        this.certificationAssessment = certificationAssessment;
        this.certificateNumbering = certificateNumbering;
        this.certificateService = certificateService;
        this.clock = clock;
    }

    @Override
    public CertificateNumber execute(RenewCertificateCommand command) {
        Certificate currentCertificate = certificateRepository.findByNumber(command.currentNumber())
                .orElseThrow(() -> new CertificateNotFoundException(command.currentNumber()));
        InspectionResult result = inspectionResultTranslator.translate(command.inspectionId()).forScope(command.scope());
        var decision = certificationAssessment.evaluate(result);
        if (!decision.isEligible()) {
            throw new NotEligibleForCertificationException(decision);
        }
        certificateService.ensureRenewableWith(currentCertificate, result);
        certificateRepository.findCurrentFor(result.assetCode(), command.scope(), decision.validity().orElseThrow().validFrom())
                .filter(existing -> !existing.number().equals(currentCertificate.number()))
                .ifPresent(existing -> { throw new ar.edu.itba.certiflow.certification.model.exceptions.AssetAlreadyCertifiedException(result.assetCode(), existing.number()); });
        Certificate renewedCertificate = certificateService.renew(currentCertificate, certificateNumbering.next(),
                decision, result, command.renewedBy(), decision.evaluatedAt());
        certificateRepository.save(currentCertificate);
        certificateRepository.save(renewedCertificate);
        return renewedCertificate.number();
    }
}
