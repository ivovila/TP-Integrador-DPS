package ar.edu.itba.certiflow.domain.inspection;

import java.time.LocalDateTime;
import java.util.List;

import ar.edu.itba.certiflow.domain.schema.CriterionId;
import ar.edu.itba.certiflow.domain.shared.Answer;
import ar.edu.itba.certiflow.domain.shared.Measurement;

public record InspectionRecord(CriterionId criterion, Answer answer, Measurement measurement, List<Evidence> evidences, List<String> observations, LocalDateTime recordedAt) {
    public InspectionRecord {
        evidences = List.copyOf(evidences);
        observations = List.copyOf(observations);
    }
}
