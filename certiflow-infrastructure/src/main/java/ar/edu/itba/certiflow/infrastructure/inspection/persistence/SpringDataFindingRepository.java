package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataFindingRepository extends JpaRepository<FindingEntity, String> {

    List<FindingEntity> findByAssetCode(String assetCode);

    @Query("select finding.assetCode from FindingEntity finding where finding.id = :id")
    Optional<String> findAssetCodeById(@Param("id") String id);
}
