package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
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
        return schemaVersion.sections().all().stream()
                .filter(section -> sectionTitles.contains(section.title()))
                .flatMap(section -> section.criteria().stream())
                .toList();
    }

    @Override
    public <T> T describedBy(InspectionScopeDescriber<T> describer) {
        return describer.sections(sectionTitles);
    }
}
