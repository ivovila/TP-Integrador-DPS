package ar.edu.itba.certiflow.domain.rules;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import ar.edu.itba.certiflow.domain.model.shared.Answer;
import ar.edu.itba.certiflow.domain.model.shared.Measurement;

public record MeasurementRangeRule(String magnitude, String unit,
                                   BigDecimal min, BigDecimal max, BigDecimal tolerance)
        implements ApprovalRule {

    @Override
    public CriterionOutcome evaluate(List<Answer> answers, List<Measurement> measurements) {
        return measurements.stream()
                .filter(this::accepts)
                .map(m -> classify(m.value()))
                .max(Comparator.naturalOrder())
                .orElse(CriterionOutcome.OBSERVED);
    }

    @Override
    public boolean accepts(Measurement measurement) {
        return measurement.magnitude().equals(magnitude) && measurement.unit().equals(unit);
    }

    private CriterionOutcome classify(BigDecimal value) {
        if (value.compareTo(min) >= 0 && value.compareTo(max) <= 0) {
            return CriterionOutcome.APPROVED;
        }
        if (value.compareTo(min.subtract(tolerance)) >= 0 && value.compareTo(max.add(tolerance)) <= 0) {
            return CriterionOutcome.OBSERVED;
        }
        return CriterionOutcome.REJECTED;
    }
}
