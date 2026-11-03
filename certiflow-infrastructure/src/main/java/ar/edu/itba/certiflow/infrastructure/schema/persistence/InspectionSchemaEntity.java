package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.SchemaVersionLog;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Entity
@Table(name = "inspection_schema")
public class InspectionSchemaEntity {

    @Id
    @Column(name = "asset_type")
    private String assetType;
    private String name;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_type", referencedColumnName = "asset_type", insertable = false, updatable = false)
    @OrderBy("number")
    private List<SchemaVersionEntity> versions = new ArrayList<>();

    protected InspectionSchemaEntity() {
    }

    InspectionSchemaEntity(InspectionSchema schema, SectionsDocumentMapper sectionsDocuments,
                           CodesDocumentMapper codeDocuments) {
        this.assetType = schema.assetType().name();
        this.name = schema.name();
        appendVersionsOf(schema, sectionsDocuments, codeDocuments);
    }

    /** El contenido de una versión publicada no cambia: se agregan las nuevas y se anota la cancelación de las guardadas. */
    void appendVersionsOf(InspectionSchema schema, SectionsDocumentMapper sectionsDocuments,
                          CodesDocumentMapper codeDocuments) {
        IntStream.range(0, versions.size())
                .forEach(index -> versions.get(index).recordCancellationOf(schema.versions().get(index)));
        schema.versions().stream()
                .skip(versions.size())
                .forEach(version -> versions.add(
                        new SchemaVersionEntity(assetType, version, sectionsDocuments, codeDocuments)));
    }

    InspectionSchema toDomain(SectionsDocumentMapper sectionsDocuments) {
        return new InspectionSchema(name, new AssetType(assetType),
                new SchemaVersionLog(versions.stream()
                        .map(version -> version.toDomain(sectionsDocuments))
                        .toList()));
    }
}
