package ar.edu.itba.certiflow.domain.usecase;

import java.util.List;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.ports.CertificateRepository;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.ports.FindingRepository;

public class SuspendForOverdueActions {

    private final CertificateRepository certificates;
    private final FindingRepository findings;
    private final Clock clock;
    private final EventPublisher events;

    public SuspendForOverdueActions(CertificateRepository certificates, FindingRepository findings, Clock clock,
                                    EventPublisher events) {
        this.certificates = certificates;
        this.findings = findings;
        this.clock = clock;
        this.events = events;
    }

    public List<Certificate> execute(AssetId assetId) {
        boolean overdue = findings.findByAsset(assetId).stream()
                .anyMatch(finding -> finding.isOpen() && finding.hasOverdueActions(clock.today()));
        if (!overdue) {
            return List.of();
        }
        List<Certificate> suspended = certificates.findByAsset(assetId).stream()
                .filter(certificate -> certificate.isValidOn(clock.today()))
                .toList();
        for (Certificate certificate : suspended) {
            certificate.suspend("Accion correctiva vencida sin verificar", clock.now());
            certificates.save(certificate);
            certificate.pullEvents().forEach(events::publish);
        }
        return suspended;
    }
}
