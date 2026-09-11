package ar.edu.itba.certiflow.domain.schema;
import java.util.List;

public record SchemaVersion(SchemaId schemaId, int number, String name, List<Section> sections) {

    public SchemaVersion {
        sections = List.copyOf(sections);
    }
}
