package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import java.util.List;

public record FullScope() implements InspectionScope {

    @Override
    public List<Criterion<?>> criteriaIn(SchemaVersion schemaVersion) {
        return schemaVersion.criteria();
    }
}
