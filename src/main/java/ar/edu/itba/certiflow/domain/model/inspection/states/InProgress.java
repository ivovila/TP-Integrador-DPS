package ar.edu.itba.certiflow.domain.model.inspection.states;

import ar.edu.itba.certiflow.domain.model.inspection.InspectionState;

public class InProgress implements InspectionState {

    @Override
    public void checkCanRegister() {
    }

    @Override
    public InspectionState close() {
        return new Closed();
    }
}
