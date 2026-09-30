package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.certificate.AuditedCertificateFactory;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.models.certificate.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.models.certification.CertificationDecision;
import ar.edu.itba.certiflow.models.certification.CertificationEligibility;
import ar.edu.itba.certiflow.models.certification.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.CertificateNumbering;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import java.time.Clock;
import java.time.LocalDate;

public final class IssueCertificate {

    private final InspectionRepository inspectionRepository;
    private final CertificateRepository certificateRepository;
    private final FindingRepository findingRepository;
    private final CertificationEligibility certificationEligibility;
    private final CertificateNumbering certificateNumbering;
    private final AuditedCertificateFactory certificateFactory;
    private final Clock clock;

    public IssueCertificate(InspectionRepository inspectionRepository, CertificateRepository certificateRepository,
                            FindingRepository findingRepository,
                            CertificationEligibility certificationEligibility, CertificateNumbering certificateNumbering,
                            AuditedCertificateFactory certificateFactory, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.certificateRepository = certificateRepository;
        this.findingRepository = findingRepository;
        this.certificationEligibility = certificationEligibility;
        this.certificateNumbering = certificateNumbering;
        this.certificateFactory = certificateFactory;
        this.clock = clock;
    }

    public CertificateNumber execute(InspectionId inspectionId, LocalDate validUntil, Person issuedBy) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        LocalDate today = LocalDate.now(clock);
        Asset asset = inspection.asset();
        CertificationDecision decision = certificationEligibility.evaluate(inspection, findingRepository.findByAsset(asset));
        if (!decision.isEligible()) {
            throw new NotEligibleForCertificationException(decision);
        }
        certificateRepository.findCurrentFor(asset, today).ifPresent(currentCertificate -> {
            throw new AssetAlreadyCertifiedException(asset, currentCertificate.number());
        });
        Certificate certificate = certificateFactory.create(certificateNumbering.next(), asset, inspection,
                new ValidityPeriod(today, validUntil), issuedBy, clock.instant());
        certificateRepository.save(certificate);
        return certificate.number();
    }
}
