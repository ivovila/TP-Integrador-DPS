package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;

public interface AttachEvidenceUseCase {

    void execute(InspectionId inspectionId, Criterion<?> criterion, Evidence evidence, Person attachedBy);
}
