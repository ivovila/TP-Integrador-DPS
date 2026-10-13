package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

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

    SchemaVersionEntity(String assetType, SchemaVersion version, SectionsDocumentMapper sectionsDocuments,
                        CodesDocumentMapper codeDocuments) {
        this.assetType = assetType;
        this.number = version.number();
        this.publishedAt = version.publishedAt();
        this.effectiveFrom = version.effectiveFrom();
        this.requiredSubsystems = codeDocuments.writeSubsystemCodes(version.requiredSubsystems());
        this.sections = sectionsDocuments.write(version.sections());
    }

    public String assetType() {
        return assetType;
    }

    SchemaVersion toDomain(SectionsDocumentMapper sectionsDocuments, CodesDocumentMapper codeDocuments) {
        return new SchemaVersion(number, sectionsDocuments.read(sections), publishedAt, effectiveFrom,
                codeDocuments.readSubsystemCodes(requiredSubsystems));
    }
}
