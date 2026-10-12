package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certificate.CertificateService;
import ar.edu.itba.certiflow.models.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.models.certificate.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.models.certification.CertificationEligibility;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.CertificateNumbering;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.usecases.certificate.IssueCertificate;
import java.time.Clock;
import java.time.LocalDate;

public final class IssueCertificateInteractor implements IssueCertificate {

    private final InspectionRepository inspectionRepository;
    private final CertificateRepository certificateRepository;
    private final FindingRepository findingRepository;
    private final CertificationEligibility certificationEligibility;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;
    private final Clock clock;

    public IssueCertificateInteractor(InspectionRepository inspectionRepository, CertificateRepository certificateRepository,
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
    public CertificateNumber execute(InspectionId inspectionId, LocalDate validUntil, Person issuedBy) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        LocalDate today = LocalDate.now(clock);
        AssetCode assetCode = inspection.assetCode();
        certificationEligibility.ensureEligible(inspection, findingRepository.findByAsset(assetCode));
        certificateRepository.findCurrentFor(assetCode, today).ifPresent(currentCertificate -> {
            throw new AssetAlreadyCertifiedException(assetCode, currentCertificate.number());
        });
        ValidityPeriod validity = new ValidityPeriod(today, validUntil);
        Certificate certificate = certificateService.issue(certificateNumbering.next(), assetCode, inspection, validity,
                issuedBy, clock.instant());
        certificateRepository.save(certificate);
        return certificate.number();
    }
}
