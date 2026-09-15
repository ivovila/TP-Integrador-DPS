package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.model.certificate.CertificateId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.ports.CertificateRepository;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;

public class ExpireCertificate {

    private final CertificateRepository certificates;
    private final Clock clock;
    private final EventPublisher events;

    public ExpireCertificate(CertificateRepository certificates, Clock clock, EventPublisher events) {
        this.certificates = certificates;
        this.clock = clock;
        this.events = events;
    }

    public void execute(CertificateId certificateId) {
        Certificate certificate = certificates.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("el certificado", certificateId.value()));
        certificate.expire(clock.now());
        certificates.save(certificate);
        certificate.pullEvents().forEach(events::publish);
    }
}
