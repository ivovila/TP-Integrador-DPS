package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataAssetRepository extends JpaRepository<AssetEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AssetEntity a where a.code = :code")
    Optional<AssetEntity> findLocked(@Param("code") String code);
}
