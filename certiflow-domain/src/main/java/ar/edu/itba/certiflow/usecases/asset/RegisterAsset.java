package ar.edu.itba.certiflow.usecases.asset;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.shared.Person;

public interface RegisterAsset {

    Asset execute(Asset asset, Person registeredBy);
}
