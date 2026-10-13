package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.shared.model.Person;

public record RecordAnswerCommand(InspectionId inspectionId, CriterionCode criterionCode, RawAnswer answer, Person answeredBy) {
}
