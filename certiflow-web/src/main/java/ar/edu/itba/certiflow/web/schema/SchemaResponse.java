package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "schemas", itemRelation = "schema")
public class SchemaResponse extends RepresentationModel<SchemaResponse> {

    public final String name;
    public final String assetType;
    public final int currentVersion;

    SchemaResponse(InspectionSchema schema) {
        this.name = schema.name();
        this.assetType = schema.assetType().name();
        this.currentVersion = schema.currentVersion().number();
    }
}
