package ar.edu.itba.certiflow.catalog.usecase;

import ar.edu.itba.certiflow.catalog.model.Asset;

public interface RegisterAssetUseCase {

    Asset execute(RegisterAssetCommand command);
}
