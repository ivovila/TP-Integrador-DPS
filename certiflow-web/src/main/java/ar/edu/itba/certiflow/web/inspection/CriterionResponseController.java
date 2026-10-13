package ar.edu.itba.certiflow.web.inspection;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.inspection.usecase.AddObservationCommand;
import ar.edu.itba.certiflow.inspection.usecase.AddObservationUseCase;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceCommand;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceUseCase;
import ar.edu.itba.certiflow.inspection.usecase.FindInspectionUseCase;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerCommand;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerUseCase;
import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Las respuestas de una inspección: responder, adjuntar evidencia y agregar observaciones. */
@RestController
@RequestMapping("/api/v1/inspections")
public class CriterionResponseController {

    private final FindInspectionUseCase findInspection;
    private final RecordAnswerUseCase recordAnswer;
    private final AttachEvidenceUseCase attachEvidence;
    private final AddObservationUseCase addObservation;

    public CriterionResponseController(FindInspectionUseCase findInspection,
                                       RecordAnswerUseCase recordAnswer,
                                       AttachEvidenceUseCase attachEvidence,
                                       AddObservationUseCase addObservation) {
        this.findInspection = findInspection;
        this.recordAnswer = recordAnswer;
        this.attachEvidence = attachEvidence;
        this.addObservation = addObservation;
    }

    @GetMapping("/{id}/responses")
    public CollectionModel<CriterionResponseResource> responses(@PathVariable String id) {
        InspectionView inspection = view(id);
        return CollectionModel.of(inspection.evaluate().responses().stream()
                        .map(response -> responseResource(inspection, response)).toList(),
                linkTo(methodOn(CriterionResponseController.class).responses(id)).withSelfRel());
    }

    @GetMapping("/{id}/responses/{code}")
    public CriterionResponseResource response(@PathVariable String id, @PathVariable String code) {
        InspectionView inspection = view(id);
        return responseResource(inspection, criterionResponse(inspection, code));
    }

    private static CriterionResponse<?> criterionResponse(InspectionView inspection, String code) {
        return inspection.evaluate().responses().stream()
                .filter(response -> response.criterion().code().equals(code))
                .findFirst()
                .orElseThrow(() -> new CriterionNotInSchemaException(code));
    }

    @PutMapping("/{id}/responses/{code}/answer")
    public ResponseEntity<Void> answer(@PathVariable String id, @PathVariable String code, @RequestBody AnswerRequest request,
                                       @ActingPerson Person answeredBy) {
        recordAnswer.execute(new RecordAnswerCommand(new InspectionId(id), code, RawAnswers.from(request.value()), answeredBy));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/responses/{code}/evidence")
    public ResponseEntity<EntityModel<CriterionResponseResource.EvidenceView>> attach(@PathVariable String id,
                                                                                      @PathVariable String code,
                                                                                      @RequestBody EvidenceRequest request,
                                                                                      @ActingPerson Person attachedBy) {
        attachEvidence.execute(new AttachEvidenceCommand(new InspectionId(id), code, request.toDomain(), attachedBy));
        EntityModel<CriterionResponseResource.EvidenceView> evidence = evidence(id, code, criterionResponse(view(id), code).attachments().size());
        return ResponseEntity.created(evidence.getRequiredLink("self").toUri()).body(evidence);
    }

    @GetMapping("/{id}/responses/{code}/evidence/{number}")
    public EntityModel<CriterionResponseResource.EvidenceView> evidence(@PathVariable String id, @PathVariable String code,
                                                                        @PathVariable int number) {
        CriterionResponseResource resource = new CriterionResponseResource(criterionResponse(view(id), code));
        if (number < 1 || number > resource.evidence.size()) {
            throw new ResourceNotFoundException("Criterion " + code + " has no evidence " + number);
        }
        return EntityModel.of(resource.evidence.get(number - 1),
                linkTo(methodOn(CriterionResponseController.class).evidence(id, code, number)).withSelfRel(),
                linkTo(methodOn(CriterionResponseController.class).response(id, code)).withRel("response"));
    }

    @PostMapping("/{id}/responses/{code}/observations")
    public ResponseEntity<EntityModel<CriterionResponseResource.ObservationView>> observe(@PathVariable String id,
                                                                                          @PathVariable String code,
                                                                                          @RequestBody ObservationRequest request,
                                                                                          @ActingPerson Person observedBy) {
        addObservation.execute(new AddObservationCommand(new InspectionId(id), code, request.text(), observedBy));
        EntityModel<CriterionResponseResource.ObservationView> observation =
                observation(id, code, criterionResponse(view(id), code).observations().size());
        return ResponseEntity.created(observation.getRequiredLink("self").toUri()).body(observation);
    }

    @GetMapping("/{id}/responses/{code}/observations/{number}")
    public EntityModel<CriterionResponseResource.ObservationView> observation(@PathVariable String id, @PathVariable String code,
                                                                              @PathVariable int number) {
        CriterionResponseResource resource = new CriterionResponseResource(criterionResponse(view(id), code));
        if (number < 1 || number > resource.observations.size()) {
            throw new ResourceNotFoundException("Criterion " + code + " has no observation " + number);
        }
        return EntityModel.of(resource.observations.get(number - 1),
                linkTo(methodOn(CriterionResponseController.class).observation(id, code, number)).withSelfRel(),
                linkTo(methodOn(CriterionResponseController.class).response(id, code)).withRel("response"));
    }

    private CriterionResponseResource responseResource(InspectionView inspection, CriterionResponse<?> criterionResponse) {
        String id = inspection.id().value();
        String code = criterionResponse.criterion().code();
        CriterionResponseResource resource = new CriterionResponseResource(criterionResponse);
        resource.add(linkTo(methodOn(CriterionResponseController.class).response(id, code)).withSelfRel(),
                linkTo(methodOn(InspectionController.class).find(id)).withRel("inspection"));
        if (!inspection.isClosed()) {
            resource.add(linkTo(methodOn(CriterionResponseController.class).answer(id, code, null, null)).withRel("answer"),
                    linkTo(methodOn(CriterionResponseController.class).attach(id, code, null, null)).withRel("evidence"),
                    linkTo(methodOn(CriterionResponseController.class).observe(id, code, null, null)).withRel("observations"));
        }
        return resource;
    }

    private InspectionView view(String id) {
        return findInspection.execute(new InspectionId(id));
    }
}
