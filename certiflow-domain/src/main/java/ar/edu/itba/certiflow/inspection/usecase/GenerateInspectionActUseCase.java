package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;

public interface GenerateInspectionActUseCase {

    InspectionAct execute(InspectionId inspectionId);
}
