package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.InspectionResultTranslator;
import ar.edu.itba.certiflow.certification.model.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import java.time.Clock;

public final class IssueCertificateUseCaseImpl implements IssueCertificateUseCase {

    private final InspectionResultTranslator inspectionResultTranslator;
    private final CertificateRepository certificateRepository;
    private final CertificationAssessmentService certificationAssessment;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;
    private final Clock clock;

    public IssueCertificateUseCaseImpl(InspectionResultTranslator inspectionResultTranslator, CertificateRepository certificateRepository,
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
    public CertificateNumber execute(IssueCertificateCommand command) {
        InspectionResult result = inspectionResultTranslator.translate(command.inspectionId()).forScope(command.scope());
        var decision = certificationAssessment.evaluate(result);
        if (!decision.isEligible()) {
            throw new NotEligibleForCertificationException(decision);
        }
        certificateRepository.findCurrentFor(result.assetCode(), command.scope(), decision.validity().orElseThrow().validFrom()).ifPresent(currentCertificate -> {
            throw new AssetAlreadyCertifiedException(result.assetCode(), currentCertificate.number());
        });
        Certificate certificate = certificateService.issue(certificateNumbering.next(), result, decision, command.issuedBy(),
                decision.evaluatedAt());
        certificateRepository.save(certificate);
        return certificate.number();
    }
}
