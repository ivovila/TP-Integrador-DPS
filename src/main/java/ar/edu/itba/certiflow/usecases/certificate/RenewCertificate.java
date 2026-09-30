package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateNotFoundException;
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

public final class RenewCertificate {

    private final InspectionRepository inspectionRepository;
    private final CertificateRepository certificateRepository;
    private final FindingRepository findingRepository;
    private final CertificationEligibility certificationEligibility;
    private final CertificateNumbering certificateNumbering;
    private final Clock clock;

    public RenewCertificate(InspectionRepository inspectionRepository, CertificateRepository certificateRepository,
                            FindingRepository findingRepository,
                            CertificationEligibility certificationEligibility, CertificateNumbering certificateNumbering, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.certificateRepository = certificateRepository;
        this.findingRepository = findingRepository;
        this.certificationEligibility = certificationEligibility;
        this.certificateNumbering = certificateNumbering;
        this.clock = clock;
    }

    public CertificateNumber execute(CertificateNumber currentNumber, InspectionId inspectionId, LocalDate validUntil, Person renewedBy) {
        Certificate currentCertificate = certificateRepository.findByNumber(currentNumber)
                .orElseThrow(() -> new CertificateNotFoundException(currentNumber));
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        CertificationDecision decision = certificationEligibility.evaluate(inspection, findingRepository.findByAsset(inspection.asset()));
        if (!decision.isEligible()) {
            throw new NotEligibleForCertificationException(decision);
        }
        currentCertificate.ensureRenewableWith(inspection);
        Certificate renewedCertificate = currentCertificate.renew(certificateNumbering.next(), new ValidityPeriod(LocalDate.now(clock), validUntil),
                inspection, renewedBy, clock.instant());
        certificateRepository.save(currentCertificate);
        certificateRepository.save(renewedCertificate);
        return renewedCertificate.number();
    }
}
