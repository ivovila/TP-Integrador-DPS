package ar.edu.itba.certiflow.catalog.usecase;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import java.util.List;

public interface ListAssetsUseCase {

    List<Asset> execute();

    List<Asset> execute(AssetType type);
}
