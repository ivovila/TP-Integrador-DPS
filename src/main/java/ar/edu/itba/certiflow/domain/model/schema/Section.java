package ar.edu.itba.certiflow.domain.model.schema;

import java.util.ArrayList;
import java.util.List;

public record Section(String name, List<Criterion> criteria) {

    public Section {
        criteria = List.copyOf(criteria);
    }

    public static Section named(String name) {
        return new Section(name, List.of());
    }

    public Section with(Criterion criterion) {
        List<Criterion> updated = new ArrayList<>(criteria);
        updated.add(criterion);
        return new Section(name, updated);
    }
}
