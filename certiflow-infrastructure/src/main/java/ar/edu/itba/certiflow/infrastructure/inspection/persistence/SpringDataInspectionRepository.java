package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataInspectionRepository extends JpaRepository<InspectionEntity, String> {

    List<InspectionEntity> findByAssetCode(String assetCode);

    @Query("select inspection.assetCode from InspectionEntity inspection where inspection.id = :id")
    Optional<String> findAssetCodeById(@Param("id") String id);
}
