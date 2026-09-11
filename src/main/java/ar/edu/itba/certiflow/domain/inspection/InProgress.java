package ar.edu.itba.certiflow.domain.inspection;

public class InProgress implements InspectionState {

    @Override
    public void checkCanRegister() {
    }

    @Override
    public InspectionState close() {
        return new Closed();
    }
}
