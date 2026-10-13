package ar.edu.itba.certiflow.schema.repository;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import java.util.Optional;
import ar.edu.itba.certiflow.shared.model.Page;
import ar.edu.itba.certiflow.shared.model.PageRequest;

public interface InspectionSchemaRepository {

    void save(InspectionSchema schema);

    /** Lee el esquema y lo deja bloqueado hasta el fin de la transacción: dos publicaciones no se pisan. */
    Optional<InspectionSchema> findForUpdate(AssetType assetType);

    Optional<InspectionSchema> findByAssetType(AssetType assetType);

    /** Los esquemas ordenados por tipo de activo, de a una página. */
    Page<InspectionSchema> findPage(PageRequest request);
}
