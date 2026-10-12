package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;

public interface AssignInspectionUseCase {

    InspectionId execute(AssignInspectionCommand command);
}
