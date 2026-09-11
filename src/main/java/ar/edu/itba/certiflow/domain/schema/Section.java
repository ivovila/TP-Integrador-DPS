package ar.edu.itba.certiflow.domain.schema;

import java.util.List;

public record Section(String name, List<Criterion> criteria) {

    public Section {
        criteria = List.copyOf(criteria);
    }
}
