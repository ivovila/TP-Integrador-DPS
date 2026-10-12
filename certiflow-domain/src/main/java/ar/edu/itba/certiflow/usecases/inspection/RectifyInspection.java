package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.util.List;

public interface RectifyInspection {

    /** Devuelve los hallazgos que la rectificación levanta, uno por cada no conformidad que introduce. */
    <A extends Answer> List<FindingId> execute(InspectionId inspectionId, Criterion<A> criterion, A answer, String reason,
                                               Person rectifiedBy);
}
