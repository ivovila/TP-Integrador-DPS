package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAuditEntryRepository extends JpaRepository<AuditEntryEntity, Long> {

    List<AuditEntryEntity> findBySubjectTypeAndSubjectIdOrderByIdAsc(String subjectType, String subjectId);
}
