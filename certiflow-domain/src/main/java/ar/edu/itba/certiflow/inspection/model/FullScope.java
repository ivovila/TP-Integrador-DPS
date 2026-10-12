package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import java.util.List;

public record FullScope() implements InspectionScope {

    @Override
    public List<Criterion<?>> criteriaIn(SchemaVersion schemaVersion) {
        return schemaVersion.criteria();
    }
}
