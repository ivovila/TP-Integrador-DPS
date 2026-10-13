package ar.edu.itba.certiflow.inspection.repository;

import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import java.util.List;
import java.util.Optional;

public interface InspectionRepository {

    InspectionId nextId();

    void save(Inspection inspection);

    Optional<Inspection> findById(InspectionId id);

    /** El activo de una inspección, sin cargarla: es lo que se bloquea antes de leerla para cambiarla. */
    Optional<AssetCode> assetCodeOf(InspectionId inspectionId);

    List<Inspection> findByAsset(AssetCode assetCode);

    List<Inspection> findAll();
}
