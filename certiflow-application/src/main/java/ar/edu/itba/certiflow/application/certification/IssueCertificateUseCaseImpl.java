package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
import ar.edu.itba.certiflow.certification.model.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.certification.model.CertificationEligibility;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.certification.repository.CertificateNumbering;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import java.time.Clock;
import java.time.LocalDate;

public final class IssueCertificateUseCaseImpl implements IssueCertificateUseCase {

    private final InspectionRepository inspectionRepository;
    private final CertificateRepository certificateRepository;
    private final FindingRepository findingRepository;
    private final CertificationEligibility certificationEligibility;
    private final CertificateNumbering certificateNumbering;
    private final CertificateService certificateService;
    private final Clock clock;

    public IssueCertificateUseCaseImpl(InspectionRepository inspectionRepository, CertificateRepository certificateRepository,
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
