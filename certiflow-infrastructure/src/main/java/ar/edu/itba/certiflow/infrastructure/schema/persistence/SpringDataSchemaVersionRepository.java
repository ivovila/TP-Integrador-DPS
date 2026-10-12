package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSchemaVersionRepository extends JpaRepository<SchemaVersionEntity, Long> {

    List<SchemaVersionEntity> findByNumber(int number);
}
