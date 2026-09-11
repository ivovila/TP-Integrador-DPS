package ar.edu.itba.certiflow.domain.asset;

import java.util.Map;

public record Characteristics(Map<String, String> values) {

    public Characteristics {
        values = Map.copyOf(values);
    }
}
