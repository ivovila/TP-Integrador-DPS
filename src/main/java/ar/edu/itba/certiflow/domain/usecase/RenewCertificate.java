package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.model.certificate.CertificateId;
import ar.edu.itba.certiflow.domain.model.certificate.CertificationPolicy;
import ar.edu.itba.certiflow.domain.model.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.ports.CertificateRepository;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.ports.FindingRepository;
import ar.edu.itba.certiflow.domain.ports.InspectionRepository;

public class RenewCertificate {

    private final CertificateRepository certificates;
    private final InspectionRepository inspections;
    private final FindingRepository findings;
    private final CertificationPolicy policy;
    private final Clock clock;
    private final EventPublisher events;

    public RenewCertificate(CertificateRepository certificates, InspectionRepository inspections,
                            FindingRepository findings, CertificationPolicy policy, Clock clock,
                            EventPublisher events) {
        this.certificates = certificates;
        this.inspections = inspections;
        this.findings = findings;
        this.policy = policy;
        this.clock = clock;
        this.events = events;
    }

    public Certificate execute(CertificateId certificateId, InspectionId inspectionId, ValidityPeriod validity) {
        Certificate current = certificates.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("el certificado", certificateId.value()));
        Inspection inspection = inspections.findById(inspectionId)
                .orElseThrow(() -> new NotFoundException("la inspeccion", inspectionId.value()));
        Certificate renewed = current.renew(CertificateId.generate(), inspection,
                findings.findByInspection(inspectionId), policy, validity, clock.now());
        certificates.save(current);
        certificates.save(renewed);
        current.pullEvents().forEach(events::publish);
        renewed.pullEvents().forEach(events::publish);
        return renewed;
    }
}
