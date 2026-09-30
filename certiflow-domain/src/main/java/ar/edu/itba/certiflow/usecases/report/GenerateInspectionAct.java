package ar.edu.itba.certiflow.usecases.report;

import ar.edu.itba.certiflow.usecases.report.InspectionAct;
import ar.edu.itba.certiflow.models.inspection.InspectionId;

public interface GenerateInspectionAct {

    InspectionAct execute(InspectionId inspectionId);
}
