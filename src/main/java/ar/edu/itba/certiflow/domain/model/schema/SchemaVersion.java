package ar.edu.itba.certiflow.domain.model.schema;

import java.util.List;

public record SchemaVersion(SchemaId schemaId, int number, String name, List<Section> sections) {

    public SchemaVersion {
        sections = sections.stream().map(Section::copy).toList();
    }

    @Override
    public List<Section> sections() {
        return sections.stream().map(Section::copy).toList();
    }
}
