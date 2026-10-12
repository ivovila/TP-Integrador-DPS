package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.SectionNotInSchemaException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

public record SchemaVersion(int number, List<Section> sections, Instant publishedAt) {

    public SchemaVersion {
        sections = List.copyOf(sections);
        if (sections.isEmpty()) {
            throw new InvalidSchemaException("A schema version needs at least one section");
        }
        List<String> criterionCodes = sections.stream()
                .flatMap(section -> section.criteria().stream())
                .map(criterion -> criterion.code())
                .toList();
        if (new HashSet<>(criterionCodes).size() != criterionCodes.size()) {
            throw new InvalidSchemaException("Criterion codes must be unique within a schema version");
        }
    }

    public List<Criterion<?>> criteria() {
        return sections.stream().flatMap(section -> section.criteria().stream()).toList();
    }

    public Section section(String title) {
        return sections.stream()
                .filter(section -> section.title().equals(title))
                .findFirst()
                .orElseThrow(() -> new SectionNotInSchemaException(title));
    }

    public void ensureContains(Criterion<?> criterion) {
        if (!criteria().contains(criterion)) {
            throw new CriterionNotInSchemaException(criterion);
        }
    }
}
