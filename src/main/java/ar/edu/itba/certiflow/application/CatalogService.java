package ar.edu.itba.certiflow.application;

import ar.edu.itba.certiflow.application.port.AssetRepository;
import ar.edu.itba.certiflow.application.port.SchemeRepository;
import ar.edu.itba.certiflow.domain.Asset;
import ar.edu.itba.certiflow.domain.Checks;
import ar.edu.itba.certiflow.domain.SchemeVersion;
import ar.edu.itba.certiflow.domain.Section;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class CatalogService {
    private final AssetRepository assets;
    private final SchemeRepository schemes;
    private final Clock clock;

    public CatalogService(AssetRepository assets, SchemeRepository schemes, Clock clock) {
        this.assets = Objects.requireNonNull(assets);
        this.schemes = Objects.requireNonNull(schemes);
        this.clock = Objects.requireNonNull(clock);
    }

    public Asset registerAsset(
            String name,
            String type,
            String location,
            String responsible,
            Map<String, String> characteristics) {
        var asset =
                new Asset(UUID.randomUUID(), name, type, location, responsible, characteristics);
        assets.save(asset);
        return asset;
    }

    public List<Asset> assets() {
        return List.copyOf(assets.findAll());
    }

    public SchemeVersion publish(
            UUID schemeId,
            String name,
            String assetType,
            List<Section> sections,
            int validityDays,
            String actor) {
        var previous = schemes.versions(schemeId);
        Checks.require(
                previous.stream().allMatch(v -> v.assetType().equals(assetType)),
                "A scheme cannot change its asset type");
        Checks.require(
                previous.stream().noneMatch(v -> v.publishedAt().isAfter(clock.instant())),
                "Publication time cannot move backwards");
        int next = previous.stream().mapToInt(SchemeVersion::number).max().orElse(0) + 1;
        var version =
                new SchemeVersion(
                        schemeId,
                        next,
                        name,
                        assetType,
                        sections,
                        validityDays,
                        clock.instant(),
                        actor);
        schemes.publish(version);
        return version;
    }
}
