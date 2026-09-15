package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.ports.AssetRepository;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;

public class ReassignAssetResponsible {

    private final AssetRepository assets;
    private final Clock clock;
    private final EventPublisher events;

    public ReassignAssetResponsible(AssetRepository assets, Clock clock, EventPublisher events) {
        this.assets = assets;
        this.clock = clock;
        this.events = events;
    }

    public void execute(AssetId assetId, PersonId newResponsible) {
        Asset asset = assets.getById(assetId);
        asset.reassignResponsible(newResponsible, clock.now());
        assets.save(asset);
        events.publishFrom(asset);
    }
}
