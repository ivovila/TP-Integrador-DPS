package ar.edu.itba.certiflow.application;

import ar.edu.itba.certiflow.application.port.ActionRepository;
import ar.edu.itba.certiflow.application.port.CertificateRepository;
import ar.edu.itba.certiflow.application.port.InspectionRepository;
import ar.edu.itba.certiflow.domain.Asset;
import ar.edu.itba.certiflow.domain.Certificate;
import ar.edu.itba.certiflow.domain.Checks;
import ar.edu.itba.certiflow.domain.CorrectiveAction;
import ar.edu.itba.certiflow.domain.DomainException;
import ar.edu.itba.certiflow.domain.Finding;
import ar.edu.itba.certiflow.domain.Inspection;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Business reports as immutable data. PDF/HTML rendering is a separate future adapter. */
public final class ReportService {
    public record InspectionAct(Inspection inspection, Instant generatedAt) {}

    public record FindingSummary(
            UUID inspectionId,
            int revision,
            List<Finding> findings,
            List<CorrectiveAction> actions) {
        public FindingSummary {
            findings = List.copyOf(findings);
            actions = List.copyOf(actions);
        }
    }

    public record CertificateReport(
            Certificate certificate,
            Certificate.Status status,
            Asset asset,
            UUID schemeId,
            int schemeVersion,
            Instant generatedAt,
            String statusReason) {}

    private final InspectionRepository inspections;
    private final ActionRepository actions;
    private final CertificateRepository certificates;
    private final Clock clock;

    public ReportService(
            InspectionRepository inspections,
            ActionRepository actions,
            CertificateRepository certificates,
            Clock clock) {
        this.inspections = Objects.requireNonNull(inspections);
        this.actions = Objects.requireNonNull(actions);
        this.certificates = Objects.requireNonNull(certificates);
        this.clock = Objects.requireNonNull(clock);
    }

    public InspectionAct inspectionAct(UUID id) {
        var inspection = inspection(id);
        Checks.require(
                inspection.state() == Inspection.State.CLOSED, "Act requires a closed inspection");
        return new InspectionAct(inspection, clock.instant());
    }

    public FindingSummary findings(UUID id) {
        var inspection = inspection(id);
        return new FindingSummary(
                id,
                inspection.revision(),
                inspection.findings(),
                actions.findByInspection(id).stream()
                        .filter(a -> a.finding().revision() == inspection.revision())
                        .toList());
    }

    public CertificateReport certificate(UUID id) {
        var certificate =
                certificates
                        .findById(id)
                        .orElseThrow(() -> new DomainException("Certificate not found"));
        var inspection = inspection(certificate.inspectionId());
        var status = certificate.statusAt(clock.instant(), inspection);
        String reason =
                status == Certificate.Status.SUSPENDED
                                && inspection.revision() != certificate.inspectionRevision()
                        ? "Source inspection was rectified; certificate remains linked to original"
                                + " revision"
                        : "Status derived from lifecycle, validity interval and source inspection";
        return new CertificateReport(
                certificate,
                status,
                inspection.asset(),
                inspection.schemeId(),
                inspection.version().orElseThrow().number(),
                clock.instant(),
                reason);
    }

    private Inspection inspection(UUID id) {
        return inspections
                .findById(id)
                .orElseThrow(() -> new DomainException("Inspection not found"));
    }
}
