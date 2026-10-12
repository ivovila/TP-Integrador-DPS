package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;

public interface RecordAnswerUseCase {

    <A extends Answer> void execute(InspectionId inspectionId, Criterion<A> criterion, A answer, Person answeredBy);
}
