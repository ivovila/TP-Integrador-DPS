package ar.edu.itba.certiflow.domain.rules;

import java.math.BigDecimal;

import ar.edu.itba.certiflow.domain.model.shared.Measurement;
import ar.edu.itba.certiflow.domain.model.shared.Response;

public record NumericRangeRule(String magnitude, String unit,
                               BigDecimal min, BigDecimal max, BigDecimal tolerance)
        implements CriterionRule {

    public NumericRangeRule {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("El minimo no puede superar al maximo");
        }
        if (tolerance.signum() < 0) {
            throw new IllegalArgumentException("La tolerancia no puede ser negativa");
        }
    }

    @Override
    public CriterionOutcome evaluate(Response response) {
        return response.measurement()
                .filter(this::accepts)
                .map(m -> classify(m.value()))
                .orElse(CriterionOutcome.REJECTED);
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
