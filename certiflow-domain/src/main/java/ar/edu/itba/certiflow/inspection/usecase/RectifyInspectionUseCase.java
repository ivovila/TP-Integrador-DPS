package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;

public interface RectifyInspectionUseCase {

    /** Devuelve los hallazgos que la rectificación levanta, uno por cada no conformidad que introduce. */
    <A extends Answer> List<FindingId> execute(InspectionId inspectionId, Criterion<A> criterion, A answer, String reason,
                                               Person rectifiedBy);
}
