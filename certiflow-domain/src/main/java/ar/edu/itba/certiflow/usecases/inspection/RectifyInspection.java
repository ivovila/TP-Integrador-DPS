package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;

public interface RectifyInspection {

    <A extends Answer> void execute(InspectionId inspectionId, Criterion<A> criterion, A answer, String reason,
                                    Person rectifiedBy);
}
