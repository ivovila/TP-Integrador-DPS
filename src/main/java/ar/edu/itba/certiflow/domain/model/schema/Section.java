package ar.edu.itba.certiflow.domain.model.schema;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class Section {

    private final String name;
    private final List<Criterion> criteria = new ArrayList<>();

    public Section(String name) {
        this.name = name;
    }

    public void addCriterion(Criterion criterion) {
        criteria.add(criterion);
    }

    public List<Criterion> getCriteria() {
        return List.copyOf(criteria);
    }

    public Section copy() {
        Section copy = new Section(name);
        copy.criteria.addAll(criteria);
        return copy;
    }
}
