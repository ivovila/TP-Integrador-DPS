package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.shared.Person;
import java.util.List;

public interface CloseInspection {

    List<FindingId> execute(InspectionId inspectionId, Person closedBy);
}
