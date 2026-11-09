package ar.edu.itba.certiflow.infrastructure.assistance.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataAssistantReviewRepository extends JpaRepository<AssistantReviewEntity, String> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "decisions")
    List<AssistantReviewEntity> findByInspectionIdOrderByRequestedAtAscIdAsc(String inspectionId);

    @Query("select review.inspectionId from AssistantReviewEntity review where review.id = :id")
    Optional<String> findInspectionIdById(@Param("id") String id);
}
