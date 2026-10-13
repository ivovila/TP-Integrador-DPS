package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataInspectionSchemaRepository extends JpaRepository<InspectionSchemaEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InspectionSchemaEntity s where s.assetType = :type")
    Optional<InspectionSchemaEntity> findLocked(@Param("type") String type);
}
