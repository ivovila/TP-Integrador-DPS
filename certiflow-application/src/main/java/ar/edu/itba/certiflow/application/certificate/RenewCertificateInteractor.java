package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certificate.CertificateService;
import ar.edu.itba.certiflow.models.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.models.certification.CertificationEligibility;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.CertificateNumbering;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.usecases.certificate.RenewCertificate;
import java.time.Clock;
import java.time.LocalDate;

public final class RenewCertificateInteractor implements RenewCertificate {

    private final InspectionRepository inspectionRepository;
    private final CertificateRepository certificateRepository;
    private final FindingRepository findingRepository;
    private final CertificationEligibility certificationEligibility;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;
    private final Clock clock;

    public RenewCertificateInteractor(InspectionRepository inspectionRepository, CertificateRepository certificateRepository,
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
    public CertificateNumber execute(CertificateNumber currentNumber, InspectionId inspectionId, LocalDate validUntil, Person renewedBy) {
        Certificate currentCertificate = certificateRepository.findByNumber(currentNumber)
                .orElseThrow(() -> new CertificateNotFoundException(currentNumber));
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        certificationEligibility.ensureEligible(inspection, findingRepository.findByAsset(inspection.assetCode()));
        ValidityPeriod renewalValidity = new ValidityPeriod(LocalDate.now(clock), validUntil);
        certificateService.ensureRenewableWith(currentCertificate, inspection);
        Certificate renewedCertificate = certificateService.renew(currentCertificate, certificateNumbering.next(),
                renewalValidity, inspection, renewedBy, clock.instant());
        certificateRepository.save(currentCertificate);
        certificateRepository.save(renewedCertificate);
        return renewedCertificate.number();
    }
}
