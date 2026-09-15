package ar.edu.itba.certiflow.domain.model.inspection.states;

import ar.edu.itba.certiflow.domain.model.inspection.InspectionState;

public class Rectified implements InspectionState {

    @Override
    public InspectionState rectify() {
        return this;
    }

    @Override
    public void checkCanEvaluate() {
    }
}
