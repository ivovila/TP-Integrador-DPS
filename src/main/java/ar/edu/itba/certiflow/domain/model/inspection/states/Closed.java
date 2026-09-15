package ar.edu.itba.certiflow.domain.model.inspection.states;

import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionState;
import ar.edu.itba.certiflow.domain.model.schema.SchemaVersion;

public record Closed(SchemaVersion schema, InspectionEvaluation evaluation) implements InspectionState {

    @Override
    public InspectionState rectify(InspectionEvaluation corrected) {
        return new Rectified(schema, corrected);
    }
}
