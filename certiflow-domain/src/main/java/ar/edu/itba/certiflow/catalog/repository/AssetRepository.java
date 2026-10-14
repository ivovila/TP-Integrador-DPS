package ar.edu.itba.certiflow.catalog.repository;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import java.util.List;
import java.util.Optional;

public interface AssetRepository {

    /** Da de alta un activo. Si ya hay uno con ese código falla: nunca pisa al que estaba. */
    void add(Asset asset);

    Optional<Asset> findByCode(AssetCode code);

    List<Asset> findAll();
}
