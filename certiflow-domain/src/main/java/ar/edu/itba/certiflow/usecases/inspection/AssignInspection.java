package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.shared.Person;

public interface AssignInspection {

    InspectionId execute(AssetCode assetCode, InspectionAssignment assignment, Person assignedBy);
}
