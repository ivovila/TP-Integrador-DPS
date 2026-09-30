package ar.edu.itba.certiflow.usecases.schema;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import ar.edu.itba.certiflow.models.schema.Section;
import ar.edu.itba.certiflow.models.shared.Person;
import java.util.List;

public interface PublishSchemaVersion {

    SchemaVersion execute(AssetType assetType, List<Section> sections, Person publishedBy);
}
