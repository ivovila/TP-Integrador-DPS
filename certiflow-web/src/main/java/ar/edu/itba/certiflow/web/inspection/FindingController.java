package ar.edu.itba.certiflow.web.inspection;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.web.support.Uris;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.CorrectiveAction;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.exceptions.CorrectiveActionNotInFindingException;
import ar.edu.itba.certiflow.inspection.usecase.FindFindingUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateFindingsSummaryUseCase;
import ar.edu.itba.certiflow.inspection.usecase.ListFindingsUseCase;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionCommand;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.VerifyCorrectiveActionCommand;
import ar.edu.itba.certiflow.inspection.usecase.VerifyCorrectiveActionUseCase;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import ar.edu.itba.certiflow.shared.usecase.AuditSubject;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.AuditEntryResponse;
import ar.edu.itba.certiflow.web.support.PageLinks;
import ar.edu.itba.certiflow.web.support.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FindingController {

    private final ListFindingsUseCase listFindings;
    private final FindFindingUseCase findFinding;
    private final PlanCorrectiveActionUseCase planCorrectiveAction;
    private final VerifyCorrectiveActionUseCase verifyCorrectiveAction;
    private final GenerateFindingsSummaryUseCase generateFindingsSummary;
    private final AuditHistoryUseCase auditHistory;
    private final Clock clock;

    public FindingController(ListFindingsUseCase listFindings, FindFindingUseCase findFinding,
                             PlanCorrectiveActionUseCase planCorrectiveAction, VerifyCorrectiveActionUseCase verifyCorrectiveAction,
                             GenerateFindingsSummaryUseCase generateFindingsSummary, AuditHistoryUseCase auditHistory, Clock clock) {
        this.listFindings = listFindings;
        this.findFinding = findFinding;
        this.planCorrectiveAction = planCorrectiveAction;
        this.verifyCorrectiveAction = verifyCorrectiveAction;
        this.generateFindingsSummary = generateFindingsSummary;
        this.auditHistory = auditHistory;
        this.clock = clock;
    }

    @GetMapping("/findings")
    public PagedModel<FindingResource> list(@RequestParam(required = false) String asset, @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return PageLinks.page(Optional.ofNullable(asset).map(code -> listFindings.execute(new AssetCode(code))).orElseGet(listFindings::execute).stream().map(this::resource).toList(), page, size,
                "/api/v1/findings", Uris.query("asset", asset).orElse(""));
    }

    @GetMapping("/findings/{id}")
    public FindingResource find(@PathVariable String id) {
        return resource(findFinding.execute(new FindingId(id)));
    }

    @PostMapping("/findings/{id}/corrective-actions")
    public ResponseEntity<CorrectiveActionResource> plan(@PathVariable String id, @RequestBody CorrectiveActionRequest request,
                                                         @ActingPerson Person plannedBy) {
        CorrectiveActionId actionId = planCorrectiveAction.execute(new PlanCorrectiveActionCommand(new FindingId(id),
                request.description(), request.responsible().toDomain(), request.dueDate(), plannedBy));
        CorrectiveActionResource resource = action(id, actionId.value());
        return ResponseEntity.created(resource.getRequiredLink("self").toUri()).body(resource);
    }

    @GetMapping("/findings/{id}/corrective-actions/{actionId}")
    public CorrectiveActionResource action(@PathVariable String id, @PathVariable int actionId) {
        Finding finding = findFinding.execute(new FindingId(id));
        CorrectiveActionId wanted = new CorrectiveActionId(actionId);
        return finding.actions().stream()
                .filter(action -> action.id().equals(wanted))
                .findFirst()
                .map(action -> actionResource(finding, action))
                .orElseThrow(() -> new CorrectiveActionNotInFindingException(wanted));
    }

    @PostMapping("/findings/{id}/corrective-actions/{actionId}/verifications")
    public ResponseEntity<EntityModel<CorrectiveActionResource.VerificationView>> verify(@PathVariable String id,
                                                                                         @PathVariable int actionId,
                                                                                         @RequestBody VerificationRequest request,
                                                                                         @ActingPerson Person verifier) {
        verifyCorrectiveAction.execute(new VerifyCorrectiveActionCommand(new FindingId(id), new CorrectiveActionId(actionId), verifier,
                request.verificationResult(), request.notes()));
        EntityModel<CorrectiveActionResource.VerificationView> verification =
                verification(id, actionId, action(id, actionId).verifications.size());
        return ResponseEntity.created(verification.getRequiredLink("self").toUri()).body(verification);
    }

    @GetMapping("/findings/{id}/corrective-actions/{actionId}/verifications/{number}")
    public EntityModel<CorrectiveActionResource.VerificationView> verification(@PathVariable String id, @PathVariable int actionId,
                                                                               @PathVariable int number) {
        CorrectiveActionResource action = action(id, actionId);
        if (number < 1 || number > action.verifications.size()) {
            throw new ResourceNotFoundException("Corrective action " + actionId + " has no verification " + number);
        }
        return EntityModel.of(action.verifications.get(number - 1),
                linkTo(methodOn(FindingController.class).verification(id, actionId, number)).withSelfRel(),
                linkTo(methodOn(FindingController.class).action(id, actionId)).withRel("corrective-action"));
    }

    @GetMapping("/findings/{id}/audit-entries")
    public CollectionModel<AuditEntryResponse> history(@PathVariable String id) {
        return CollectionModel.of(auditHistory.execute(new AuditSubject.OfFinding(new FindingId(id))).stream()
                        .map(AuditEntryResponse::of).toList(),
                linkTo(methodOn(FindingController.class).history(id)).withSelfRel());
    }

    @GetMapping("/assets/{code}/findings-summary")
    public FindingsSummaryResource summary(@PathVariable String code) {
        FindingsSummaryResource summary = new FindingsSummaryResource(generateFindingsSummary.execute(new AssetCode(code)));
        summary.add(linkTo(methodOn(FindingController.class).summary(code)).withSelfRel(),
                Link.of(Uris.path("assets", code)).withRel("asset"));
        return summary;
    }

    private FindingResource resource(Finding finding) {
        String id = finding.id().value();
        FindingResource resource = new FindingResource(finding,
                finding.actions().stream().map(action -> actionResource(finding, action)).toList());
        resource.add(linkTo(methodOn(FindingController.class).find(id)).withSelfRel(),
                Link.of(Uris.path("inspections", finding.inspectionId().value())).withRel("inspection"),
                Link.of(Uris.path("assets", finding.assetCode().value())).withRel("asset"),
                linkTo(methodOn(FindingController.class).history(id)).withRel("audit-entries"));
        if (finding.isOpen()) {
            resource.add(linkTo(methodOn(FindingController.class).plan(id, null, null)).withRel("corrective-actions"));
        }
        return resource;
    }

    private CorrectiveActionResource actionResource(Finding finding, CorrectiveAction action) {
        String id = finding.id().value();
        int actionId = action.id().value();
        CorrectiveActionResource resource = new CorrectiveActionResource(action, LocalDate.now(clock));
        resource.add(linkTo(methodOn(FindingController.class).action(id, actionId)).withSelfRel(),
                linkTo(methodOn(FindingController.class).find(id)).withRel("finding"));
        if (action.isOpen()) {
            resource.add(linkTo(methodOn(FindingController.class).verify(id, actionId, null, null)).withRel("verifications"));
        }
        return resource;
    }
}
