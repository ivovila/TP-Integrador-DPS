package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesJson;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.stream.Collectors;

@Entity
@Table(name = "schema_version")
public class SchemaVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "asset_type")
    private String assetType;
    private int number;
    @Column(name = "published_at")
    private Instant publishedAt;
    private String sections;
    @Column(name = "effective_from")
    private Instant effectiveFrom;
    @Column(name = "required_subsystems")
    private String requiredSubsystems;

    protected SchemaVersionEntity() {
    }

    static SchemaVersionEntity from(String assetType, SchemaVersion version) {
        SchemaVersionEntity entity = new SchemaVersionEntity();
        entity.assetType = assetType;
        entity.number = version.number();
        entity.publishedAt = version.publishedAt();
        entity.effectiveFrom = version.effectiveFrom();
        entity.requiredSubsystems = CodesJson.write(version.requiredSubsystems().stream().map(code -> code.value()).sorted().toList());
        entity.sections = SectionsJson.write(version.sections());
        return entity;
    }

    public String assetType() {
        return assetType;
    }

    public SchemaVersion toDomain() {
        return new SchemaVersion(number, SectionsJson.read(sections), publishedAt, effectiveFrom, CodesJson.read(requiredSubsystems).stream().map(SubsystemCode::new).collect(Collectors.toSet()));
    }

    public Long getId() {
        return id;
    }

    int number() {
        return number;
    }
}
