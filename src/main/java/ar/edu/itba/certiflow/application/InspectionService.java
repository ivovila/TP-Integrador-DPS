package ar.edu.itba.certiflow.application;

import ar.edu.itba.certiflow.application.port.AssetRepository;
import ar.edu.itba.certiflow.application.port.InspectionRepository;
import ar.edu.itba.certiflow.application.port.SchemeRepository;
import ar.edu.itba.certiflow.domain.Checks;
import ar.edu.itba.certiflow.domain.DomainException;
import ar.edu.itba.certiflow.domain.Inspection;
import ar.edu.itba.certiflow.domain.SchemeVersion;
import ar.edu.itba.certiflow.domain.Submission;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class InspectionService {
    private final AssetRepository assets;
    private final SchemeRepository schemes;
    private final InspectionRepository inspections;
    private final Clock clock;

    public InspectionService(
            AssetRepository assets,
            SchemeRepository schemes,
            InspectionRepository inspections,
            Clock clock) {
        this.assets = Objects.requireNonNull(assets);
        this.schemes = Objects.requireNonNull(schemes);
        this.inspections = Objects.requireNonNull(inspections);
        this.clock = Objects.requireNonNull(clock);
    }

    public Inspection assign(
            UUID assetId,
            UUID schemeId,
            String inspector,
            LocalDate date,
            String scope,
            String actor) {
        var asset =
                assets.findById(assetId).orElseThrow(() -> new DomainException("Asset not found"));
        Checks.require(
                schemes.versions(schemeId).stream()
                        .anyMatch(v -> v.assetType().equals(asset.type())),
                "No applicable published scheme");
        var inspection =
                Inspection.assign(
                        UUID.randomUUID(),
                        asset,
                        schemeId,
                        inspector,
                        date,
                        scope,
                        actor,
                        clock.instant());
        inspections.save(inspection);
        return inspection;
    }

    public Inspection get(UUID id) {
        return inspections
                .findById(id)
                .orElseThrow(() -> new DomainException("Inspection not found"));
    }

    public Inspection start(UUID id, String actor) {
        var inspection = get(id);
        var version =
                schemes.versions(inspection.schemeId()).stream()
                        .filter(v -> !v.publishedAt().isAfter(clock.instant()))
                        .max(Comparator.comparingInt(SchemeVersion::number))
                        .orElseThrow(() -> new DomainException("No effective published version"));
        return update(id, i -> i.start(version, actor, clock.instant()));
    }

    public Inspection record(UUID id, String code, Submission submission, String actor) {
        return update(id, i -> i.record(code, submission, actor, clock.instant()));
    }

    public Inspection evaluate(UUID id, String actor) {
        return update(id, i -> i.evaluate(actor, clock.instant()));
    }

    public Inspection close(UUID id, String actor) {
        return update(id, i -> i.close(actor, clock.instant()));
    }

    public Inspection rectify(UUID id, String reason, String actor) {
        return update(id, i -> i.rectify(reason, actor, clock.instant()));
    }

    private Inspection update(UUID id, UnaryOperator<Inspection> operation) {
        var updated = operation.apply(get(id));
        inspections.save(updated);
        return updated;
    }
}
