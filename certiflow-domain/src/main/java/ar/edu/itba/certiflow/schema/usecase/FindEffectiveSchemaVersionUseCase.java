package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import java.time.Instant;

public interface FindEffectiveSchemaVersionUseCase {

    SchemaVersion execute(AssetType assetType, Instant resolvedAt);
}
