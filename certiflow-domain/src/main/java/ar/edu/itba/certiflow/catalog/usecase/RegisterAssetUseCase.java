package ar.edu.itba.certiflow.catalog.usecase;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.shared.model.Person;

public interface RegisterAssetUseCase {

    Asset execute(Asset asset, Person registeredBy);
}
