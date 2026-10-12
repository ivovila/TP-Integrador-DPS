package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;

public interface AddObservationUseCase {

    void execute(InspectionId inspectionId, Criterion<?> criterion, String observationText, Person observedBy);
}
