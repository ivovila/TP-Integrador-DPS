package ar.edu.itba.certiflow.domain.model.inspection.states;

import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionState;
import ar.edu.itba.certiflow.domain.model.schema.SchemaVersion;

public record InProgress(SchemaVersion schema) implements InspectionState {

    @Override
    public void checkCanRegister() {
    }

    @Override
    public InspectionState close(InspectionEvaluation evaluation) {
        return new Closed(schema, evaluation);
    }
}
