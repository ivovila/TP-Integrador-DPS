package ar.edu.itba.certiflow.usecases.schema;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.models.schema.Section;
import ar.edu.itba.certiflow.models.shared.Person;
import java.util.List;

public interface CreateInspectionSchema {

    InspectionSchema execute(String name, AssetType assetType, List<Section> sections, Person createdBy);
}
