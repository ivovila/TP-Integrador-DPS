package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataInspectionRepository extends JpaRepository<InspectionEntity, String> {
}
