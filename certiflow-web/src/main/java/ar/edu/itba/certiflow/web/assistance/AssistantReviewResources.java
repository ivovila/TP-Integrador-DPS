package ar.edu.itba.certiflow.web.assistance;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.Suggestion;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.inspection.usecase.FindInspectionUseCase;
import ar.edu.itba.certiflow.web.inspection.CriterionResponseController;
import ar.edu.itba.certiflow.web.inspection.InspectionController;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import org.springframework.stereotype.Component;

@Component
public class AssistantReviewResources {
    private final FindInspectionUseCase findInspection;

    public AssistantReviewResources(FindInspectionUseCase findInspection) {
        this.findInspection = findInspection;
    }

    public AssistantReviewResponse one(AssistantReview review) {
        return resource(review, severities(findInspection.execute(review.inspectionId())));
    }

    public List<AssistantReviewResponse> list(String inspectionId, List<AssistantReview> reviews) {
        InspectionView inspection = findInspection.execute(new InspectionId(inspectionId));
        Map<CriterionCode, String> severities = severities(inspection);
        return reviews.stream().map(review -> resource(review, severities)).toList();
    }

    public SuggestionResponse suggestion(AssistantReview review, int position) {
        return suggestionResource(review, review.suggestion(position), severities(findInspection.execute(review.inspectionId())));
    }
    private AssistantReviewResponse resource(AssistantReview review, Map<CriterionCode, String> severities) {
        AssistantReviewResponse response = new AssistantReviewResponse(review, review.suggestions().stream()
                .map(suggestion -> suggestionResource(review, suggestion, severities)).toList());
        response.add(linkTo(methodOn(AssistantReviewController.class).find(review.id().value())).withSelfRel(),
                linkTo(methodOn(InspectionController.class).find(review.inspectionId().value())).withRel("inspection"),
                linkTo(methodOn(AssistantReviewController.class).history(review.id().value())).withRel("audit-entries"));
        return response;
    }

    private Map<CriterionCode, String> severities(InspectionView inspection) {
        return inspection.schemaVersion().criteria().stream()
                .collect(Collectors.toMap(criterion -> criterion.code(), criterion -> criterion.severity().code()));
    }

    private SuggestionResponse suggestionResource(AssistantReview review, Suggestion suggestion, Map<CriterionCode, String> severities) {
        String id = review.id().value();
        int position = suggestion.position();
        String severity = java.util.Objects.requireNonNull(severities.get(suggestion.proposal().criterion()),
                "Suggestion criterion missing from its inspection schema");
        SuggestionResponse response = new SuggestionResponse(suggestion, severity);
        response.add(linkTo(methodOn(AssistantReviewController.class).suggestion(id, position)).withSelfRel(),
                linkTo(methodOn(AssistantReviewController.class).find(id)).withRel("review"),
                linkTo(methodOn(CriterionResponseController.class)
                        .response(review.inspectionId().value(), suggestion.proposal().criterion().value())).withRel("criterion"));
        if (suggestion.isPending()) {
            response.add(linkTo(methodOn(AssistantReviewController.class).accept(id, position, null)).withRel("acceptance"),
                    linkTo(methodOn(AssistantReviewController.class).dismiss(id, position, null, null)).withRel("dismissal"));
        }
        return response;
    }
}
