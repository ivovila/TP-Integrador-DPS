package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;

public interface PublishSchemaVersionUseCase {

    SchemaVersion execute(AssetType assetType, List<Section> sections, Person publishedBy);
}
