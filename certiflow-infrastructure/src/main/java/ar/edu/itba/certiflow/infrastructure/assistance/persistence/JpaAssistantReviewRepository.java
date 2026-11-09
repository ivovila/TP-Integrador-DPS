package ar.edu.itba.certiflow.infrastructure.assistance.persistence;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewFactory;
import ar.edu.itba.certiflow.assistance.model.AssistantReviewSnapshot;
import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.assistance.repository.AssistantReviewRepository;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAssistantReviewRepository implements AssistantReviewRepository {

    private final SpringDataAssistantReviewRepository reviews;
    private final ProposalsDocumentMapper proposalsDocuments;
    private final AssistantReviewFactory factory = new AssistantReviewFactory();

    public JpaAssistantReviewRepository(SpringDataAssistantReviewRepository reviews,
                                        ProposalsDocumentMapper proposalsDocuments) {
        this.reviews = reviews;
        this.proposalsDocuments = proposalsDocuments;
    }

    @Override
    public ReviewId nextId() {
        return new ReviewId(UUID.randomUUID().toString());
    }

    @Override
    public void save(AssistantReview review) {
        AssistantReviewSnapshot snapshot = review.snapshot();
        reviews.findById(snapshot.id().value()).ifPresentOrElse(
                existing -> existing.copyDecisionsFrom(snapshot),
                () -> reviews.save(new AssistantReviewEntity(snapshot, proposalsDocuments)));
    }

    @Override
    public Optional<AssistantReview> findById(ReviewId id) {
        return reviews.findById(id.value()).map(this::restore);
    }

    @Override
    public Optional<InspectionId> inspectionOf(ReviewId id) {
        return reviews.findInspectionIdById(id.value()).map(InspectionId::new);
    }

    @Override
    public List<AssistantReview> findByInspection(InspectionId inspectionId) {
        return reviews.findByInspectionIdOrderByRequestedAtAscIdAsc(inspectionId.value()).stream().map(this::restore).toList();
    }

    private AssistantReview restore(AssistantReviewEntity entity) {
        return factory.restore(entity.toSnapshot(proposalsDocuments));
    }
}
