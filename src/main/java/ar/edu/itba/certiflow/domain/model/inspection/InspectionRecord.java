package ar.edu.itba.certiflow.domain.model.inspection;

import java.time.LocalDateTime;
import java.util.List;

import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.shared.Answer;
import ar.edu.itba.certiflow.domain.model.shared.Evidence;
import ar.edu.itba.certiflow.domain.model.shared.Measurement;

public record InspectionRecord(CriterionId criterion, Answer answer, Measurement measurement, List<Evidence> evidences, List<String> observations, LocalDateTime recordedAt) {
    public InspectionRecord {
        evidences = List.copyOf(evidences);
        observations = List.copyOf(observations);
    }
}
