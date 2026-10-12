package ar.edu.itba.certiflow.catalog.usecase;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import java.util.List;
import java.util.Optional;

public interface ListAssetsUseCase {

    List<Asset> execute(Optional<AssetType> type);
}
