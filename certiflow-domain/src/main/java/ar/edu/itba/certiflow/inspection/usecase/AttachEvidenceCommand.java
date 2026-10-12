package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.shared.model.Person;

public record AttachEvidenceCommand(InspectionId inspectionId, String criterionCode, Evidence evidence, Person attachedBy) {
}
