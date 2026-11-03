package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.NoEffectiveSchemaVersionException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaEffectiveDateConflictException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaVersionNotFoundException;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record SchemaVersionLog(List<SchemaVersion> versions) {

    public SchemaVersionLog {
        List<SchemaVersion> orderedVersions = List.copyOf(versions);
        versions = orderedVersions;
        if (versions.isEmpty()) {
            throw new InvalidSchemaException("A schema needs at least one version");
        }
        List<SchemaVersion> activeVersions = versions.stream().filter(version -> !version.isCancelled()).toList();
        if (activeVersions.isEmpty()) {
            throw new InvalidSchemaException("A schema needs at least one version that is not cancelled");
        }
        for (int index = 1; index < activeVersions.size(); index++) {
            if (!activeVersions.get(index).effectiveFrom().isAfter(activeVersions.get(index - 1).effectiveFrom())) {
                throw new SchemaEffectiveDateConflictException();
            }
        }
        if (IntStream.range(0, orderedVersions.size())
                .anyMatch(index -> orderedVersions.get(index).number() != index + 1)) {
            throw new InvalidSchemaException("Schema versions must be numbered consecutively from 1");
        }
    }

    /** El registro de un esquema nuevo: su primera versión rige desde que se publica. */
    public SchemaVersionLog(List<Section> sections, Instant publishedAt, Set<SubsystemCode> requiredSubsystems) {
        this(List.of(new SchemaVersion(1, new SchemaSections(sections), publishedAt, publishedAt)));
        latestPublished().ensureRequires(requiredSubsystems);
    }

    public SchemaVersionLog append(List<Section> sections, Instant publishedAt, Instant effectiveFrom,
            Set<SubsystemCode> requiredSubsystems) {
        SchemaVersion nextVersion = new SchemaVersion(versions.size() + 1, new SchemaSections(sections), publishedAt,
                effectiveFrom);
        nextVersion.ensureRequires(requiredSubsystems);
        return new SchemaVersionLog(Stream.concat(versions.stream(), Stream.of(nextVersion)).toList());
    }

    SchemaVersionLog cancel(AssetType assetType, int number, Stamp cancelled, String reason) {
        SchemaVersion cancelledVersion = version(assetType, number).cancelled(cancelled, reason);
        return new SchemaVersionLog(versions.stream()
                .map(version -> version.number() == number ? cancelledVersion : version)
                .toList());
    }

    SchemaVersion version(AssetType assetType, int number) {
        return versions.stream().filter(version -> version.number() == number).findFirst()
                .orElseThrow(() -> new SchemaVersionNotFoundException(assetType, number));
    }

    public SchemaVersion versionAt(Instant instant) {
        return versions.stream().filter(version -> version.isInForceAt(instant))
                .reduce((previous, next) -> next)
                .orElseThrow(() -> new NoEffectiveSchemaVersionException(instant));
    }

    public SchemaVersion latestPublished() {
        return versions.stream().filter(version -> !version.isCancelled())
                .reduce((previous, next) -> next)
                .orElseThrow();
    }
}
