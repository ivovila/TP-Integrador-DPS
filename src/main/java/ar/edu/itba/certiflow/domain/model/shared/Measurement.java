package ar.edu.itba.certiflow.domain.model.shared;

import java.math.BigDecimal;

public record Measurement(String magnitude, BigDecimal value, String unit) implements RecordedValue {

    @Override
    public boolean supersedes(RecordedValue previous) {
        return previous instanceof Measurement other
                && other.magnitude.equals(magnitude)
                && other.unit.equals(unit);
    }
}
