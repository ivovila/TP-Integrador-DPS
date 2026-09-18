package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.inspection.CriterionResponse;
import ar.edu.itba.certiflow.domain.inspection.InspectionRecord;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public interface FindingGenerator {

    Finding generate(CriterionResponse<?> nonConformity, InspectionRecord inspection, Person by, Instant at);
}
