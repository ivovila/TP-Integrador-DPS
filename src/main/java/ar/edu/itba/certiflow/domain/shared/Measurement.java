package ar.edu.itba.certiflow.domain.shared;

import java.math.BigDecimal;

public record Measurement(String magnitude, BigDecimal value, String unit) {
}
