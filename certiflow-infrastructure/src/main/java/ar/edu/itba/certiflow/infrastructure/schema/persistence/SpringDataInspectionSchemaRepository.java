package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataInspectionSchemaRepository extends JpaRepository<InspectionSchemaEntity, String> {
}
