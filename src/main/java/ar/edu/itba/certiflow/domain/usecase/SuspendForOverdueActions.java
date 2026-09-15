package ar.edu.itba.certiflow.domain.usecase;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
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
        List<Finding> assetFindings = findings.findByAsset(assetId);
        List<Certificate> suspended = new ArrayList<>();
        for (Certificate certificate : certificates.findByAsset(assetId)) {
            if (certificate.suspendIfActionsOverdue(assetFindings, clock.now())) {
                certificates.save(certificate);
                certificate.pullEvents().forEach(events::publish);
                suspended.add(certificate);
            }
        }
        return suspended;
    }
}
