package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;
import java.util.List;
import java.util.Set;

public record SectionsScope(Set<String> sectionTitles) implements InspectionScope {

    public SectionsScope {
        sectionTitles = Set.copyOf(sectionTitles);
        if (sectionTitles.isEmpty()) {
            throw new RequiredValueMissingException("A sections scope must name at least one section");
        }
    }

    @Override
    public List<Criterion<?>> criteriaIn(SchemaVersion schemaVersion) {
        sectionTitles.forEach(schemaVersion::section);
        return schemaVersion.sections().stream()
                .filter(section -> sectionTitles.contains(section.title()))
                .flatMap(section -> section.criteria().stream())
                .toList();
    }
}
