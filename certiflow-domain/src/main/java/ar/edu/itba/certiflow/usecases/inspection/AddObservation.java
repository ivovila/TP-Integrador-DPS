package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;

public interface AddObservation {

    void execute(InspectionId inspectionId, Criterion<?> criterion, String observationText, Person observedBy);
}
