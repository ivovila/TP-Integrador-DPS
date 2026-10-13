package ar.edu.itba.certiflow.web.inspection;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.FindInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.InspectionHistoryUseCase;
import ar.edu.itba.certiflow.inspection.usecase.ListInspectionsUseCase;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.web.certification.EligibilityController;
import ar.edu.itba.certiflow.web.security.AllowedRole;
import ar.edu.itba.certiflow.web.security.ReadableBy;
import ar.edu.itba.certiflow.web.security.StaffRole;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.AuditEntryResponse;
import ar.edu.itba.certiflow.web.support.InvalidRequestException;
import ar.edu.itba.certiflow.web.support.PageLinks;
import ar.edu.itba.certiflow.web.support.Uris;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.hateoas.CollectionModel;
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

/** Asignar, buscar y listar inspecciones, y su bitácora. */
@RestController
@ReadableBy({StaffRole.INSPECTION_COORDINATOR, StaffRole.INSPECTOR, StaffRole.CERTIFICATION_OFFICER})
@RequestMapping("/api/v1/inspections")
public class InspectionController {

    private static final Map<String, Boolean> CLOSED_BY_STATUS = Map.of("open", false, "closed", true);

    private final AssignInspectionUseCase assignInspection;
    private final FindInspectionUseCase findInspection;
    private final ListInspectionsUseCase listInspections;
    private final InspectionHistoryUseCase inspectionHistory;

    public InspectionController(AssignInspectionUseCase assignInspection,
                                FindInspectionUseCase findInspection,
                                ListInspectionsUseCase listInspections,
                                InspectionHistoryUseCase inspectionHistory) {
        this.assignInspection = assignInspection;
        this.findInspection = findInspection;
        this.listInspections = listInspections;
        this.inspectionHistory = inspectionHistory;
    }

    @AllowedRole(StaffRole.INSPECTION_COORDINATOR)
    @PostMapping
    public ResponseEntity<InspectionResponse> assign(@RequestBody AssignmentRequest request, @ActingPerson Person assignedBy) {
        InspectionId id = assignInspection.execute(request.toCommand(assignedBy));
        InspectionResponse response = resource(findInspection.execute(id));
        return ResponseEntity.created(response.getRequiredLink("self").toUri()).body(response);
    }

    @GetMapping
    public PagedModel<InspectionResponse> list(@RequestParam(required = false) String asset,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Optional<Boolean> closed = Optional.ofNullable(status).map(InspectionController::closedFor);
        List<InspectionResponse> inspections = Optional.ofNullable(asset).map(code -> listInspections.execute(new AssetCode(code))).orElseGet(listInspections::execute).stream()
                .filter(inspection -> closed.map(wanted -> inspection.isClosed() == wanted).orElse(true))
                .map(this::resource)
                .toList();
        String query = Stream.of(Uris.query("asset", asset), Uris.query("status", status))
                .flatMap(Optional::stream)
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        return PageLinks.page(inspections, page, size, "/api/v1/inspections", query);
    }

    @GetMapping("/{id}")
    public InspectionResponse find(@PathVariable String id) {
        return resource(view(id));
    }

    @GetMapping("/{id}/audit-entries")
    public CollectionModel<AuditEntryResponse> history(@PathVariable String id) {
        return CollectionModel.of(inspectionHistory.execute(new InspectionId(id)).stream()
                        .map(AuditEntryResponse::of).toList(),
                linkTo(methodOn(InspectionController.class).history(id)).withSelfRel());
    }

    private InspectionView view(String id) {
        return findInspection.execute(new InspectionId(id));
    }

    private InspectionResponse resource(InspectionView inspection) {
        String id = inspection.id().value();
        String assetCode = inspection.assetCode().value();
        String assetType = inspection.assetType().name();
        InspectionResponse response = new InspectionResponse(inspection);
        response.add(linkTo(methodOn(InspectionController.class).find(id)).withSelfRel(),
                Link.of(Uris.path("assets", assetCode)).withRel("asset"),
                Link.of(Uris.path("schemas", assetType, "versions", String.valueOf(inspection.schemaVersion().number())))
                        .withRel("schema-version"),
                linkTo(methodOn(CriterionResponseController.class).responses(id)).withRel("responses"),
                linkTo(methodOn(RevisionController.class).revisions(id)).withRel("revisions"),
                linkTo(methodOn(EligibilityController.class).eligibility(id, null)).withRel("certification-eligibility"),
                linkTo(methodOn(InspectionController.class).history(id)).withRel("audit-entries"));
        if (inspection.isClosed()) {
            response.add(linkTo(methodOn(RevisionController.class).rectify(id, null, null)).withRel("rectifications"),
                    linkTo(methodOn(RevisionController.class).act(id)).withRel("act"),
                    linkTo(methodOn(RevisionController.class).revision(id, 1, null)).withRel("original-revision"));
        } else {
            response.add(linkTo(methodOn(RevisionController.class).close(id, null)).withRel("closure"));
        }
        return response;
    }

    private static boolean closedFor(String status) {
        return Optional.ofNullable(CLOSED_BY_STATUS.get(status))
                .orElseThrow(() -> new InvalidRequestException("status must be open or closed"));
    }
}
