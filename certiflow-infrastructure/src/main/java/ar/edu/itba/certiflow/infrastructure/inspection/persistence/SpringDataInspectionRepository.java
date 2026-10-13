package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataInspectionRepository extends JpaRepository<InspectionEntity, String> {

    List<InspectionEntity> findByAssetCode(String assetCode);
}
