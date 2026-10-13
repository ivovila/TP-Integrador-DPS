package ar.edu.itba.certiflow.web.inspection;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.inspection.model.Revision;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.FindInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.GenerateInspectionActUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionUseCase;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.ResourceNotFoundException;
import ar.edu.itba.certiflow.web.support.Uris;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/** Cerrar y rectificar una inspección, sus revisiones selladas y su acta. */
@RestController
@RequestMapping("/api/v1/inspections")
public class RevisionController {

    private final FindInspectionUseCase findInspection;
    private final CloseInspectionUseCase closeInspection;
    private final RectifyInspectionUseCase rectifyInspection;
    private final GenerateInspectionActUseCase generateInspectionAct;

    public RevisionController(FindInspectionUseCase findInspection,
                              CloseInspectionUseCase closeInspection,
                              RectifyInspectionUseCase rectifyInspection,
                              GenerateInspectionActUseCase generateInspectionAct) {
        this.findInspection = findInspection;
        this.closeInspection = closeInspection;
        this.rectifyInspection = rectifyInspection;
        this.generateInspectionAct = generateInspectionAct;
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
                linkTo(methodOn(RevisionController.class).revisions(id)).withSelfRel());
    }

    @GetMapping("/{id}/revisions/{number}")
    public ResponseEntity<RevisionResource> revision(@PathVariable String id, @PathVariable int number, WebRequest request) {
        String etag = Uris.etag("inspection", id, String.valueOf(number));
        return view(id).revisions().asList().stream()
                .filter(revision -> revision.number() == number)
                .findFirst()
                .map(revision -> request.checkNotModified(etag)
                        ? ResponseEntity.status(304).eTag(etag).<RevisionResource>build()
                        : ResponseEntity.ok().eTag(etag).cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                                .body(revisionResource(id, revision)))
                .orElseThrow(() -> new ResourceNotFoundException("Inspection " + id + " has no revision " + number));
    }

    @GetMapping("/{id}/act")
    public InspectionActResource act(@PathVariable String id) {
        InspectionActResource act = new InspectionActResource(generateInspectionAct.execute(new InspectionId(id)));
        act.add(linkTo(methodOn(RevisionController.class).act(id)).withSelfRel(),
                linkTo(methodOn(InspectionController.class).find(id)).withRel("inspection"));
        return act;
    }

    private ResponseEntity<RevisionResource> createdRevision(String id, List<FindingId> raised) {
        InspectionView inspection = view(id);
        RevisionResource revision = revisionResource(id, inspection.revisions().last());
        raised.forEach(finding -> revision.add(Link.of(Uris.path("findings", finding.value())).withRel("findings")));
        URI location = revision.getRequiredLink("self").toUri();
        return ResponseEntity.created(location).body(revision);
    }

    private RevisionResource revisionResource(String id, Revision revision) {
        RevisionResource resource = new RevisionResource(revision);
        resource.add(linkTo(methodOn(RevisionController.class).revision(id, revision.number(), null)).withSelfRel(),
                linkTo(methodOn(InspectionController.class).find(id)).withRel("inspection"));
        return resource;
    }

    private InspectionView view(String id) {
        return findInspection.execute(new InspectionId(id));
    }
}
