package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSchemaVersionRepository extends JpaRepository<SchemaVersionEntity, Long> {

    List<SchemaVersionEntity> findByNumberAndPublishedAt(int number, Instant publishedAt);
}
