package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataFindingRepository extends JpaRepository<FindingEntity, String> {

    List<FindingEntity> findByAssetCode(String assetCode);
}
