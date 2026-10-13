package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.UnreadableStoredDataException;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import org.springframework.stereotype.Component;

/** Lo que la persistencia de otros contextos necesita de las versiones guardadas: su fila y su contenido. */
@Component
public class StoredSchemaVersions {

    private final SpringDataSchemaVersionRepository schemaVersions;
    private final SectionsDocumentMapper sectionsDocuments;
    private final CodesDocumentMapper codeDocuments;

    public StoredSchemaVersions(SpringDataSchemaVersionRepository schemaVersions,
                                SectionsDocumentMapper sectionsDocuments, CodesDocumentMapper codeDocuments) {
        this.schemaVersions = schemaVersions;
        this.sectionsDocuments = sectionsDocuments;
        this.codeDocuments = codeDocuments;
    }

    public SchemaVersionEntity entityOf(AssetType assetType, int versionNumber) {
        return schemaVersions.findByAssetTypeAndNumber(assetType.name(), versionNumber)
                .orElseThrow(() -> new UnreadableStoredDataException("schema version",
                        assetType.name() + " " + versionNumber));
    }

    public SchemaVersion versionStoredIn(SchemaVersionEntity entity) {
        return entity.toDomain(sectionsDocuments, codeDocuments);
    }
}
