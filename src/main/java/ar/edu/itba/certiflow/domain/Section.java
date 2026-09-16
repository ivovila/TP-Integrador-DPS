package ar.edu.itba.certiflow.domain;

import java.util.List;

public record Section(String name, List<Criterion> criteria) {
    public Section {
        name = Checks.text(name, "name");
        criteria = List.copyOf(criteria);
        Checks.require(!criteria.isEmpty(), "A section needs criteria");
    }
}
