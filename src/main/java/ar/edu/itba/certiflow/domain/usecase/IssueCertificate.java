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

public class IssueCertificate {

    private final InspectionRepository inspections;
    private final FindingRepository findings;
    private final CertificateRepository certificates;
    private final CertificationPolicy policy;
    private final Clock clock;
    private final EventPublisher events;

    public IssueCertificate(InspectionRepository inspections, FindingRepository findings,
                            CertificateRepository certificates, CertificationPolicy policy, Clock clock,
                            EventPublisher events) {
        this.inspections = inspections;
        this.findings = findings;
        this.certificates = certificates;
        this.policy = policy;
        this.clock = clock;
        this.events = events;
    }

    public Certificate execute(InspectionId inspectionId, ValidityPeriod validity) {
        Inspection inspection = inspections.findById(inspectionId)
                .orElseThrow(() -> new NotFoundException("la inspeccion", inspectionId.value()));
        Certificate certificate = Certificate.issue(CertificateId.generate(), inspection.getEvaluation(),
                findings.findByInspection(inspectionId), policy, validity, clock.now());
        certificates.save(certificate);
        certificate.pullEvents().forEach(events::publish);
        return certificate;
    }
}
