package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.usecase.RenewCertificateCommand;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.model.CertificationEligibility;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import java.time.Clock;
import java.time.LocalDate;

public final class RenewCertificateUseCaseImpl implements RenewCertificateUseCase {

    private final InspectionRepository inspectionRepository;
    private final CertificateRepository certificateRepository;
    private final FindingRepository findingRepository;
    private final CertificationEligibility certificationEligibility;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;
    private final Clock clock;

    public RenewCertificateUseCaseImpl(InspectionRepository inspectionRepository, CertificateRepository certificateRepository,
                                      FindingRepository findingRepository,
                                      CertificationEligibility certificationEligibility, CertificateNumbering certificateNumbering, CertificateService certificateService, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.certificateRepository = certificateRepository;
        this.findingRepository = findingRepository;
        this.certificationEligibility = certificationEligibility;
        this.certificateNumbering = certificateNumbering;
        this.certificateService = certificateService;
        this.clock = clock;
    }

    @Override
    public CertificateNumber execute(RenewCertificateCommand command) {
        Certificate currentCertificate = certificateRepository.findByNumber(command.currentNumber())
                .orElseThrow(() -> new CertificateNotFoundException(command.currentNumber()));
        Inspection inspection = inspectionRepository.findById(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        certificationEligibility.ensureEligible(inspection, findingRepository.findByAsset(inspection.assetCode()));
        ValidityPeriod renewalValidity = new ValidityPeriod(LocalDate.now(clock), command.validUntil());
        certificateService.ensureRenewableWith(currentCertificate, inspection);
        Certificate renewedCertificate = certificateService.renew(currentCertificate, certificateNumbering.next(),
                renewalValidity, inspection, command.renewedBy(), clock.instant());
        certificateRepository.save(currentCertificate);
        certificateRepository.save(renewedCertificate);
        return renewedCertificate.number();
    }
}
