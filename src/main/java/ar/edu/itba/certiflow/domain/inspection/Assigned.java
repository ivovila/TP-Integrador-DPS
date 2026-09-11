package ar.edu.itba.certiflow.domain.inspection;

public class Assigned implements InspectionState {

    @Override
    public InspectionState start() {
        return new InProgress();
    }
}
