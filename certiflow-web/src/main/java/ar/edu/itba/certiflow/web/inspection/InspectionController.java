package ar.edu.itba.certiflow.web.inspection;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.usecase.FindAssetUseCase;
import ar.edu.itba.certiflow.inspection.usecase.ListInspectionsUseCase;
import ar.edu.itba.certiflow.web.support.InvalidRequestException;
import ar.edu.itba.certiflow.web.support.PageLinks;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.RequestParam;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationUseCase;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.inspection.model.Revision;
import ar.edu.itba.certiflow.inspection.usecase.AddObservationCommand;
import ar.edu.itba.certiflow.inspection.usecase.AddObservationUseCase;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceCommand;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceUseCase;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.FindInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateInspectionActUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerCommand;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionUseCase;
import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import ar.edu.itba.certiflow.shared.usecase.AuditSubject;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.AuditEntryResponse;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping("/api/v1/inspections")
public class InspectionController {

    private final AssignInspectionUseCase assignInspection;
    private final FindInspectionUseCase findInspection;
    private final RecordAnswerUseCase recordAnswer;
    private final AttachEvidenceUseCase attachEvidence;
    private final AddObservationUseCase addObservation;
    private final CloseInspectionUseCase closeInspection;
    private final RectifyInspectionUseCase rectifyInspection;
    private final GenerateInspectionActUseCase generateInspectionAct;
    private final EvaluateCertificationUseCase evaluateCertification;
    private final AuditHistoryUseCase auditHistory;
    private final FindAssetUseCase findAsset;
    private final ListInspectionsUseCase listInspections;

    public InspectionController(AssignInspectionUseCase assignInspection, FindInspectionUseCase findInspection,
                                RecordAnswerUseCase recordAnswer, AttachEvidenceUseCase attachEvidence,
                                AddObservationUseCase addObservation, CloseInspectionUseCase closeInspection,
                                RectifyInspectionUseCase rectifyInspection, GenerateInspectionActUseCase generateInspectionAct,
                                EvaluateCertificationUseCase evaluateCertification, AuditHistoryUseCase auditHistory,
                                FindAssetUseCase findAsset, ListInspectionsUseCase listInspections) {
        this.assignInspection = assignInspection;
        this.findInspection = findInspection;
        this.recordAnswer = recordAnswer;
        this.attachEvidence = attachEvidence;
        this.addObservation = addObservation;
        this.closeInspection = closeInspection;
        this.rectifyInspection = rectifyInspection;
        this.generateInspectionAct = generateInspectionAct;
        this.evaluateCertification = evaluateCertification;
        this.auditHistory = auditHistory;
        this.findAsset = findAsset;
        this.listInspections = listInspections;
    }

    @PostMapping
    public ResponseEntity<InspectionResponse> assign(@RequestBody AssignmentRequest request, @ActingPerson Person assignedBy) {
        InspectionId id = assignInspection.execute(request.toCommand(assignedBy));
        InspectionResponse response = resource(findInspection.execute(id));
        return ResponseEntity.created(response.getRequiredLink("self").toUri()).body(response);
    }

    @GetMapping
    public PagedModel<InspectionResponse> list(@RequestParam Optional<String> asset, @RequestParam Optional<String> status,
                                               @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Optional<Boolean> closed = status.map(InspectionController::closedFor);
        List<InspectionResponse> inspections = listInspections.execute(asset.map(AssetCode::new)).stream()
                .filter(inspection -> closed.map(wanted -> inspection.isClosed() == wanted).orElse(true))
                .map(this::resource)
                .toList();
        String query = Stream.of(asset.map(code -> "asset=" + code), status.map(value -> "status=" + value))
                .flatMap(Optional::stream)
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        return PageLinks.page(inspections, page, size, "/api/v1/inspections", query);
    }

    private static boolean closedFor(String status) {
        return switch (status) {
            case "open" -> false;
            case "closed" -> true;
            default -> throw new InvalidRequestException("status must be open or closed");
        };
    }

    @GetMapping("/{id}")
    public InspectionResponse find(@PathVariable String id) {
        return resource(view(id));
    }

    @GetMapping("/{id}/responses")
    public CollectionModel<CriterionResponseResource> responses(@PathVariable String id) {
        InspectionView inspection = view(id);
        return CollectionModel.of(inspection.evaluate().responses().stream()
                        .map(response -> responseResource(inspection, response)).toList(),
                linkTo(methodOn(InspectionController.class).responses(id)).withSelfRel());
    }

    @GetMapping("/{id}/responses/{code}")
    public CriterionResponseResource response(@PathVariable String id, @PathVariable String code) {
        InspectionView inspection = view(id);
        return inspection.evaluate().responses().stream()
                .filter(response -> response.criterion().code().equals(code))
                .findFirst()
                .map(response -> responseResource(inspection, response))
                .orElseThrow(() -> new CriterionNotInSchemaException(code));
    }

    @PutMapping("/{id}/responses/{code}/answer")
    public ResponseEntity<Void> answer(@PathVariable String id, @PathVariable String code, @RequestBody AnswerRequest request,
                                       @ActingPerson Person answeredBy) {
        recordAnswer.execute(new RecordAnswerCommand(new InspectionId(id), code, RawAnswers.from(request.value()), answeredBy));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/responses/{code}/evidence")
    public ResponseEntity<CriterionResponseResource> attach(@PathVariable String id, @PathVariable String code,
                                                            @RequestBody EvidenceRequest request, @ActingPerson Person attachedBy) {
        attachEvidence.execute(new AttachEvidenceCommand(new InspectionId(id), code, request.toDomain(), attachedBy));
        return createdResponse(id, code);
    }

    @PostMapping("/{id}/responses/{code}/observations")
    public ResponseEntity<CriterionResponseResource> observe(@PathVariable String id, @PathVariable String code,
                                                             @RequestBody ObservationRequest request, @ActingPerson Person observedBy) {
        addObservation.execute(new AddObservationCommand(new InspectionId(id), code, request.text(), observedBy));
        return createdResponse(id, code);
    }

    @PostMapping("/{id}/closure")
    public ResponseEntity<RevisionResource> close(@PathVariable String id, @ActingPerson Person closedBy) {
        List<FindingId> raised = closeInspection.execute(new CloseInspectionCommand(new InspectionId(id), closedBy));
        return createdRevision(id, raised);
    }

    @PostMapping("/{id}/rectifications")
    public ResponseEntity<RevisionResource> rectify(@PathVariable String id, @RequestBody RectificationRequest request,
                                                    @ActingPerson Person rectifiedBy) {
        List<FindingId> raised = rectifyInspection.execute(new RectifyInspectionCommand(new InspectionId(id), request.criterionCode(),
                RawAnswers.from(request.value()), request.reason(), rectifiedBy));
        return createdRevision(id, raised);
    }

    @GetMapping("/{id}/revisions")
    public CollectionModel<RevisionResource> revisions(@PathVariable String id) {
        return CollectionModel.of(view(id).revisions().asList().stream().map(revision -> revisionResource(id, revision)).toList(),
                linkTo(methodOn(InspectionController.class).revisions(id)).withSelfRel());
    }

    @GetMapping("/{id}/revisions/{number}")
    public ResponseEntity<RevisionResource> revision(@PathVariable String id, @PathVariable int number, WebRequest request) {
        String etag = "\"inspection-" + id + "-r" + number + "\"";
        return view(id).revisions().asList().stream()
                .filter(revision -> revision.number() == number)
                .findFirst()
                .map(revision -> request.checkNotModified(etag)
                        ? ResponseEntity.status(304).eTag(etag).<RevisionResource>build()
                        : ResponseEntity.ok().eTag(etag).cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                                .body(revisionResource(id, revision)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/act")
    public InspectionActResource act(@PathVariable String id) {
        InspectionActResource act = new InspectionActResource(generateInspectionAct.execute(new InspectionId(id)));
        act.add(linkTo(methodOn(InspectionController.class).act(id)).withSelfRel(),
                linkTo(methodOn(InspectionController.class).find(id)).withRel("inspection"));
        return act;
    }

    @GetMapping("/{id}/certification-eligibility")
    public EligibilityResource eligibility(@PathVariable String id) {
        EligibilityResource eligibility = new EligibilityResource(evaluateCertification.execute(new InspectionId(id)));
        eligibility.add(linkTo(methodOn(InspectionController.class).eligibility(id)).withSelfRel(),
                linkTo(methodOn(InspectionController.class).find(id)).withRel("inspection"));
        if (eligibility.eligible) {
            eligibility.add(Link.of("/api/v1/certificates").withRel("certificates"));
        }
        return eligibility;
    }

    @GetMapping("/{id}/audit-entries")
    public CollectionModel<AuditEntryResponse> history(@PathVariable String id) {
        return CollectionModel.of(auditHistory.execute(new AuditSubject.OfInspection(new InspectionId(id))).stream()
                        .map(AuditEntryResponse::of).toList(),
                linkTo(methodOn(InspectionController.class).history(id)).withSelfRel());
    }

    private InspectionView view(String id) {
        return findInspection.execute(new InspectionId(id));
    }

    private ResponseEntity<CriterionResponseResource> createdResponse(String id, String code) {
        CriterionResponseResource resource = response(id, code);
        return ResponseEntity.created(resource.getRequiredLink("self").toUri()).body(resource);
    }

    private ResponseEntity<RevisionResource> createdRevision(String id, List<FindingId> raised) {
        InspectionView inspection = view(id);
        RevisionResource revision = revisionResource(id, inspection.revisions().last());
        raised.forEach(finding -> revision.add(Link.of("/api/v1/findings/" + finding.value()).withRel("findings")));
        URI location = revision.getRequiredLink("self").toUri();
        return ResponseEntity.created(location).body(revision);
    }

    private InspectionResponse resource(InspectionView inspection) {
        String id = inspection.id().value();
        String assetCode = inspection.assetCode().value();
        String assetType = findAsset.execute(inspection.assetCode()).type().name();
        InspectionResponse response = new InspectionResponse(inspection);
        response.add(linkTo(methodOn(InspectionController.class).find(id)).withSelfRel(),
                Link.of("/api/v1/assets/" + assetCode).withRel("asset"),
                Link.of("/api/v1/schemas/" + assetType.replace(" ", "%20") + "/versions/" + inspection.schemaVersion().number())
                        .withRel("schema-version"),
                linkTo(methodOn(InspectionController.class).responses(id)).withRel("responses"),
                linkTo(methodOn(InspectionController.class).revisions(id)).withRel("revisions"),
                linkTo(methodOn(InspectionController.class).eligibility(id)).withRel("certification-eligibility"),
                linkTo(methodOn(InspectionController.class).history(id)).withRel("audit-entries"));
        if (inspection.isClosed()) {
            response.add(linkTo(methodOn(InspectionController.class).rectify(id, null, null)).withRel("rectifications"),
                    linkTo(methodOn(InspectionController.class).act(id)).withRel("act"),
                    linkTo(methodOn(InspectionController.class).revision(id, 1, null)).withRel("original-revision"));
        } else {
            response.add(linkTo(methodOn(InspectionController.class).close(id, null)).withRel("closure"));
        }
        return response;
    }

    private CriterionResponseResource responseResource(InspectionView inspection, CriterionResponse<?> criterionResponse) {
        String id = inspection.id().value();
        String code = criterionResponse.criterion().code();
        CriterionResponseResource resource = new CriterionResponseResource(criterionResponse);
        resource.add(linkTo(methodOn(InspectionController.class).response(id, code)).withSelfRel(),
                linkTo(methodOn(InspectionController.class).find(id)).withRel("inspection"));
        if (!inspection.isClosed()) {
            resource.add(linkTo(methodOn(InspectionController.class).answer(id, code, null, null)).withRel("answer"),
                    linkTo(methodOn(InspectionController.class).attach(id, code, null, null)).withRel("evidence"),
                    linkTo(methodOn(InspectionController.class).observe(id, code, null, null)).withRel("observations"));
        }
        return resource;
    }

    private RevisionResource revisionResource(String id, Revision revision) {
        RevisionResource resource = new RevisionResource(revision);
        resource.add(linkTo(methodOn(InspectionController.class).revision(id, revision.number(), null)).withSelfRel(),
                linkTo(methodOn(InspectionController.class).find(id)).withRel("inspection"));
        return resource;
    }
}
