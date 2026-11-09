package ar.edu.itba.certiflow.web.assistance;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.assistance.model.AssistantReview;
import ar.edu.itba.certiflow.assistance.model.ReviewId;
import ar.edu.itba.certiflow.assistance.usecase.AcceptSuggestionCommand;
import ar.edu.itba.certiflow.assistance.usecase.AcceptSuggestionUseCase;
import ar.edu.itba.certiflow.assistance.usecase.AssistantReviewHistoryUseCase;
import ar.edu.itba.certiflow.assistance.usecase.DismissSuggestionCommand;
import ar.edu.itba.certiflow.assistance.usecase.DismissSuggestionUseCase;
import ar.edu.itba.certiflow.assistance.usecase.FindAssistantReviewUseCase;
import ar.edu.itba.certiflow.assistance.usecase.ListAssistantReviewsUseCase;
import ar.edu.itba.certiflow.assistance.usecase.RequestAssistantReviewCommand;
import ar.edu.itba.certiflow.assistance.usecase.RequestAssistantReviewUseCase;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.web.inspection.InspectionController;
import ar.edu.itba.certiflow.web.security.AllowedRole;
import ar.edu.itba.certiflow.web.security.ReadableBy;
import ar.edu.itba.certiflow.web.security.StaffRole;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.AuditEntryResponse;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** La revisión y sus decisiones son recursos propios; aceptarlas no modifica la inspección. */
@RestController
@ReadableBy({StaffRole.INSPECTION_COORDINATOR, StaffRole.INSPECTOR, StaffRole.CERTIFICATION_OFFICER})
@RequestMapping("/api/v1")
public class AssistantReviewController {

    private final RequestAssistantReviewUseCase requestReview;
    private final FindAssistantReviewUseCase findReview;
    private final ListAssistantReviewsUseCase listReviews;
    private final AcceptSuggestionUseCase acceptSuggestion;
    private final DismissSuggestionUseCase dismissSuggestion;
    private final AssistantReviewHistoryUseCase reviewHistory;
    private final AssistantReviewResources resources;

    public AssistantReviewController(RequestAssistantReviewUseCase requestReview, FindAssistantReviewUseCase findReview,
            ListAssistantReviewsUseCase listReviews, AcceptSuggestionUseCase acceptSuggestion,
            DismissSuggestionUseCase dismissSuggestion, AssistantReviewHistoryUseCase reviewHistory,
            AssistantReviewResources resources) {
        this.requestReview = requestReview;
        this.findReview = findReview;
        this.listReviews = listReviews;
        this.acceptSuggestion = acceptSuggestion;
        this.dismissSuggestion = dismissSuggestion;
        this.reviewHistory = reviewHistory;
        this.resources = resources;
    }

    @AllowedRole(StaffRole.INSPECTOR)
    @PostMapping("/inspections/{id}/assistant-reviews")
    public ResponseEntity<AssistantReviewResponse> request(@PathVariable String id, @ActingPerson Person requestedBy) {
        ReviewId reviewId = requestReview.execute(new RequestAssistantReviewCommand(new InspectionId(id), requestedBy));
        AssistantReviewResponse response = find(reviewId.value());
        return ResponseEntity.created(response.getRequiredLink("self").toUri()).body(response);
    }

    @GetMapping("/inspections/{id}/assistant-reviews")
    public CollectionModel<AssistantReviewResponse> list(@PathVariable String id) {
        return CollectionModel.of(resources.list(id, listReviews.execute(new InspectionId(id))),
                linkTo(methodOn(AssistantReviewController.class).list(id)).withSelfRel(),
                linkTo(methodOn(InspectionController.class).find(id)).withRel("inspection"));
    }

    @GetMapping("/assistant-reviews/{id}")
    public AssistantReviewResponse find(@PathVariable String id) {
        return resources.one(findReview.execute(new ReviewId(id)));
    }

    @GetMapping("/assistant-reviews/{id}/suggestions/{position}")
    public SuggestionResponse suggestion(@PathVariable String id, @PathVariable int position) {
        AssistantReview review = findReview.execute(new ReviewId(id));
        return resources.suggestion(review, position);
    }

    @AllowedRole(StaffRole.INSPECTOR)
    @PostMapping("/assistant-reviews/{id}/suggestions/{position}/acceptance")
    public ResponseEntity<EntityModel<DecisionResponse>> accept(@PathVariable String id, @PathVariable int position,
            @ActingPerson Person acceptedBy) {
        acceptSuggestion.execute(new AcceptSuggestionCommand(new ReviewId(id), position, acceptedBy));
        EntityModel<DecisionResponse> response = acceptance(id, position);
        return ResponseEntity.created(response.getRequiredLink("self").toUri()).body(response);
    }

    @AllowedRole(StaffRole.INSPECTOR)
    @PostMapping("/assistant-reviews/{id}/suggestions/{position}/dismissal")
    public ResponseEntity<EntityModel<DecisionResponse>> dismiss(@PathVariable String id, @PathVariable int position,
            @RequestBody DismissalRequest request, @ActingPerson Person dismissedBy) {
        dismissSuggestion.execute(new DismissSuggestionCommand(new ReviewId(id), position, request.reason(), dismissedBy));
        EntityModel<DecisionResponse> response = dismissal(id, position);
        return ResponseEntity.created(response.getRequiredLink("self").toUri()).body(response);
    }

    @GetMapping("/assistant-reviews/{id}/suggestions/{position}/acceptance")
    public EntityModel<DecisionResponse> acceptance(@PathVariable String id, @PathVariable int position) {
        return EntityModel.of(decision(id, position, DecisionResource.ACCEPTANCE),
                linkTo(methodOn(AssistantReviewController.class).acceptance(id, position)).withSelfRel(),
                linkTo(methodOn(AssistantReviewController.class).suggestion(id, position)).withRel("suggestion"));
    }

    @GetMapping("/assistant-reviews/{id}/suggestions/{position}/dismissal")
    public EntityModel<DecisionResponse> dismissal(@PathVariable String id, @PathVariable int position) {
        return EntityModel.of(decision(id, position, DecisionResource.DISMISSAL),
                linkTo(methodOn(AssistantReviewController.class).dismissal(id, position)).withSelfRel(),
                linkTo(methodOn(AssistantReviewController.class).suggestion(id, position)).withRel("suggestion"));
    }

    @GetMapping("/assistant-reviews/{id}/audit-entries")
    public CollectionModel<AuditEntryResponse> history(@PathVariable String id) {
        findReview.execute(new ReviewId(id));
        return CollectionModel.of(reviewHistory.execute(new ReviewId(id)).stream().map(AuditEntryResponse::of).toList(),
                linkTo(methodOn(AssistantReviewController.class).history(id)).withSelfRel());
    }

    private DecisionResponse decision(String id, int position, DecisionResource resource) {
        return findReview.execute(new ReviewId(id)).suggestion(position).decision().describedBy(resource);
    }
}
