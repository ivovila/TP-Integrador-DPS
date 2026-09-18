package ar.edu.itba.certiflow.domain.schema;

import ar.edu.itba.certiflow.domain.schema.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.domain.schema.exceptions.InvalidSchemaException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Inmutable: publicar una versión nueva crea otro objeto, por lo que una inspección
 * conserva exactamente las reglas con las que fue iniciada.
 */
public record SchemaVersion(int number, List<Section> sections, Instant publishedAt) {

    public SchemaVersion {
        Objects.requireNonNull(publishedAt, "publishedAt");
        sections = List.copyOf(sections);
        if (sections.isEmpty()) {
            throw new InvalidSchemaException("A schema version needs at least one section");
        }
        List<String> codes = sections.stream()
                .flatMap(section -> section.criteria().stream())
                .map(criterion -> criterion.code())
                .toList();
        if (new HashSet<>(codes).size() != codes.size()) {
            throw new InvalidSchemaException("Criterion codes must be unique within a schema version");
        }
    }

    public List<Criterion<?>> criteria() {
        return sections.stream().flatMap(section -> section.criteria().stream()).toList();
    }

    public void ensureContains(Criterion<?> criterion) {
        if (!criteria().contains(criterion)) {
            throw new CriterionNotInSchemaException(criterion);
        }
    }
}
