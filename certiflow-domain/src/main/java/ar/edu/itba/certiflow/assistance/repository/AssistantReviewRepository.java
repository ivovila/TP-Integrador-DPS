package ar.edu.itba.certiflow.assistance.repository;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.util.List;
import java.util.Optional;

public interface AssistantReviewRepository {

    ReviewId nextId();

    void save(AssistantReview review);

    Optional<AssistantReview> findById(ReviewId id);

    /** La inspección de una revisión, sin cargarla: lleva al activo que se bloquea antes de decidir una sugerencia. */
    Optional<InspectionId> inspectionOf(ReviewId id);

    /** Las revisiones de una inspección, de la más vieja a la más nueva. */
    List<AssistantReview> findByInspection(InspectionId inspectionId);
}
