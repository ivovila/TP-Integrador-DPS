package ar.edu.itba.certiflow.models.schema;

import ar.edu.itba.certiflow.models.schema.exceptions.InvalidSchemaException;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record SchemaVersionLog(List<SchemaVersion> versions) {

    public SchemaVersionLog {
        versions = List.copyOf(versions);
        if (versions.isEmpty()) {
            throw new InvalidSchemaException("A schema needs at least one version");
        }
        if (!isNumberedConsecutively(versions)) {
            throw new InvalidSchemaException("Schema versions must be numbered consecutively from 1");
        }
    }

    public static SchemaVersionLog startingWith(List<Section> sections, Instant publishedAt) {
        return new SchemaVersionLog(List.of(new SchemaVersion(1, sections, publishedAt)));
    }

    public SchemaVersionLog append(List<Section> sections, Instant publishedAt) {
        SchemaVersion nextVersion = new SchemaVersion(versions.size() + 1, sections, publishedAt);
        return new SchemaVersionLog(Stream.concat(versions.stream(), Stream.of(nextVersion)).toList());
    }

    public SchemaVersion current() {
        return versions.getLast();
    }

    private static boolean isNumberedConsecutively(List<SchemaVersion> versions) {
        return IntStream.range(0, versions.size()).allMatch(index -> versions.get(index).number() == index + 1);
    }
}
