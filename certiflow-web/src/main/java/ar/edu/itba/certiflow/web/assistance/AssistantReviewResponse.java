package ar.edu.itba.certiflow.web.assistance;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.assistance.model.RejectedProposal;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.time.Instant;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "assistant-reviews", itemRelation = "assistant-review")
public class AssistantReviewResponse extends RepresentationModel<AssistantReviewResponse> {

    public record RejectionResponse(String criterionCode, String estimatedSeverity, String rationale, List<String> quotes, String reason) {
        static RejectionResponse of(RejectedProposal rejected) {
            return new RejectionResponse(rejected.proposal().criterion().value(), rejected.proposal().estimatedSeverity().code(),
                    rejected.proposal().rationale(), rejected.proposal().quotes().stream().map(Quote::text).toList(), rejected.reason().name());
        }
    }

    public final String id;
    public final String inspectionId;
    public final String promptVersion;
    public final String modelId;
    public final PersonPayload requestedBy;
    public final Instant requestedAt;
    public final List<SuggestionResponse> suggestions;
    public final List<RejectionResponse> rejectedProposals;

    AssistantReviewResponse(AssistantReview review, List<SuggestionResponse> suggestions) {
        this.id = review.id().value();
        this.inspectionId = review.inspectionId().value();
        this.promptVersion = review.promptVersion().value();
        this.modelId = review.modelId().value();
        this.requestedBy = PersonPayload.of(review.requested().author());
        this.requestedAt = review.requested().instant();
        this.suggestions = List.copyOf(suggestions);
        this.rejectedProposals = review.rejectedProposals().stream().map(RejectionResponse::of).toList();
    }
}
