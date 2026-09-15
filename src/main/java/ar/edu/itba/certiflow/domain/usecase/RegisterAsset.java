package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.asset.AssetType;
import ar.edu.itba.certiflow.domain.model.asset.Characteristics;
import ar.edu.itba.certiflow.domain.model.asset.Location;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.ports.AssetRepository;

public class RegisterAsset {

    private final AssetRepository assets;

    public RegisterAsset(AssetRepository assets) {
        this.assets = assets;
    }

    public Asset execute(AssetType type, Location location, PersonId responsible, Characteristics characteristics) {
        Asset asset = new Asset(AssetId.generate(), type, location, responsible, characteristics);
        assets.save(asset);
        return asset;
    }
}
