package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;

public interface CloseInspectionUseCase {

    List<FindingId> execute(InspectionId inspectionId, Person closedBy);
}
