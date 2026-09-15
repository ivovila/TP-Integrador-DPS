package ar.edu.itba.certiflow.domain.model.shared;

import java.math.BigDecimal;

public record Measurement(String magnitude, BigDecimal value, String unit) {
}
