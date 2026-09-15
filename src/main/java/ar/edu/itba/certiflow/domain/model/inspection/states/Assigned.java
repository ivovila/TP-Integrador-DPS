package ar.edu.itba.certiflow.domain.model.inspection.states;

import ar.edu.itba.certiflow.domain.model.inspection.InspectionState;
import ar.edu.itba.certiflow.domain.model.schema.SchemaVersion;

public record Assigned() implements InspectionState {

    @Override
    public InspectionState start(SchemaVersion schema) {
        return new InProgress(schema);
    }
}
