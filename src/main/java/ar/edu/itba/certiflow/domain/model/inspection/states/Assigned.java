package ar.edu.itba.certiflow.domain.model.inspection.states;

import ar.edu.itba.certiflow.domain.model.inspection.InspectionState;

public class Assigned implements InspectionState {

    @Override
    public InspectionState start() {
        return new InProgress();
    }
}
