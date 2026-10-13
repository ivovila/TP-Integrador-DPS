package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.NoEffectiveSchemaVersionException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaEffectiveDateConflictException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record SchemaVersionLog(List<SchemaVersion> versions) {

    public SchemaVersionLog {
        versions = List.copyOf(versions);
        if (versions.isEmpty()) {
            throw new InvalidSchemaException("A schema needs at least one version");
        }
        for (int index = 1; index < versions.size(); index++) {
            if (!versions.get(index).effectiveFrom().isAfter(versions.get(index - 1).effectiveFrom())) {
                throw new SchemaEffectiveDateConflictException();
            }
        }
        if (!isNumberedConsecutively(versions)) {
            throw new InvalidSchemaException("Schema versions must be numbered consecutively from 1");
        }
    }

    public static SchemaVersionLog startingWith(List<Section> sections, Instant publishedAt) {
        return new SchemaVersionLog(List.of(new SchemaVersion(1, sections, publishedAt)));
    }

    public SchemaVersionLog append(List<Section> sections, Instant publishedAt) {
        return append(sections, publishedAt, publishedAt);
    }

    public SchemaVersionLog append(List<Section> sections, Instant publishedAt, Instant effectiveFrom) {
        return append(sections, publishedAt, effectiveFrom, Set.of());
    }

    public SchemaVersionLog append(List<Section> sections, Instant publishedAt, Instant effectiveFrom,
            Set<SubsystemCode> requiredSubsystems) {
        SchemaVersion nextVersion = new SchemaVersion(versions.size() + 1, sections, publishedAt, effectiveFrom, requiredSubsystems);
        return new SchemaVersionLog(Stream.concat(versions.stream(), Stream.of(nextVersion)).toList());
    }

    public SchemaVersion versionAt(Instant at) {
        return versions.stream().filter(version -> !version.effectiveFrom().isAfter(at))
                .reduce((previous, next) -> next)
                .orElseThrow(() -> new NoEffectiveSchemaVersionException(at));
    }

    public SchemaVersion latestPublished() {
        return versions.getLast();
    }

    private static boolean isNumberedConsecutively(List<SchemaVersion> versions) {
        return IntStream.range(0, versions.size()).allMatch(index -> versions.get(index).number() == index + 1);
    }
}
