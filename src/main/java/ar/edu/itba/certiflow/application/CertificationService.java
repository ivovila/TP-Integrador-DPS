package ar.edu.itba.certiflow.application;

import ar.edu.itba.certiflow.application.port.ActionRepository;
import ar.edu.itba.certiflow.application.port.CertificateRepository;
import ar.edu.itba.certiflow.application.port.InspectionRepository;
import ar.edu.itba.certiflow.domain.Certificate;
import ar.edu.itba.certiflow.domain.Checks;
import ar.edu.itba.certiflow.domain.DomainException;
import ar.edu.itba.certiflow.domain.Inspection;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class CertificationService {
    private final InspectionRepository inspections;
    private final ActionRepository actions;
    private final CertificateRepository certificates;
    private final Clock clock;

    public CertificationService(
            InspectionRepository inspections,
            ActionRepository actions,
            CertificateRepository certificates,
            Clock clock) {
        this.inspections = Objects.requireNonNull(inspections);
        this.actions = Objects.requireNonNull(actions);
        this.certificates = Objects.requireNonNull(certificates);
        this.clock = Objects.requireNonNull(clock);
    }

    public Certificate issue(UUID inspectionId, String actor) {
        var inspection = inspection(inspectionId);
        requireIssuable(inspection, null);
        var certificate =
                Certificate.issue(
                        UUID.randomUUID(),
                        inspection,
                        actions.findByInspection(inspection.id()),
                        actor,
                        clock.instant());
        certificates.save(certificate);
        return certificate;
    }

    public Certificate suspend(UUID id, String reason, String actor) {
        var current = get(id);
        var updated =
                current.suspend(reason, actor, clock.instant(), inspection(current.inspectionId()));
        certificates.save(updated);
        return updated;
    }

    public Certificate renew(UUID oldId, UUID newInspectionId, String actor) {
        var old = get(oldId);
        var inspection = inspection(newInspectionId);
        Checks.require(
                status(oldId) != Certificate.Status.SUPERSEDED, "Certificate already renewed");
        Checks.require(
                old.assetId().equals(inspection.asset().id()), "Renewal must refer to same asset");
        Checks.require(
                !old.inspectionId().equals(newInspectionId), "Renewal requires a new inspection");
        Checks.require(
                inspection.startedAt().isPresent()
                        && !inspection.startedAt().orElseThrow().isBefore(old.issuedAt()),
                "Renewal inspection predates certificate");
        requireIssuable(inspection, oldId);
        var replacement =
                Certificate.issue(
                        UUID.randomUUID(),
                        inspection,
                        actions.findByInspection(inspection.id()),
                        actor,
                        clock.instant());
        var superseded = old.supersede(replacement.id(), actor, clock.instant());
        certificates.save(superseded);
        certificates.save(replacement);
        return replacement;
    }

    public Certificate get(UUID id) {
        return certificates
                .findById(id)
                .orElseThrow(() -> new DomainException("Certificate not found"));
    }

    public Certificate.Status status(UUID id) {
        var c = get(id);
        return c.statusAt(clock.instant(), inspection(c.inspectionId()));
    }

    private Inspection inspection(UUID id) {
        return inspections
                .findById(id)
                .orElseThrow(() -> new DomainException("Inspection not found"));
    }

    private void requireIssuable(Inspection inspection, UUID replacing) {
        for (var existing : certificates.findByAsset(inspection.asset().id())) {
            Checks.require(
                    !(existing.inspectionId().equals(inspection.id())
                            && existing.inspectionRevision() == inspection.revision()),
                    "Revision already certified");
            Checks.require(
                    existing.id().equals(replacing)
                            || existing.statusAt(
                                            clock.instant(), inspection(existing.inspectionId()))
                                    != Certificate.Status.ACTIVE,
                    "Asset already has an active certificate; use renewal");
        }
    }
}
