package ar.edu.itba.certiflow.web.certification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationCommand;
import ar.edu.itba.certiflow.certification.usecase.EvaluateCertificationUseCase;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.web.inspection.InspectionController;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Si una inspección permite certificar el activo o uno de sus subsistemas. */
@RestController
@RequestMapping("/api/v1/inspections")
public class EligibilityController {

    private final EvaluateCertificationUseCase evaluateCertification;

    public EligibilityController(EvaluateCertificationUseCase evaluateCertification) {
        this.evaluateCertification = evaluateCertification;
    }

    @GetMapping("/{id}/certification-eligibility")
    public EligibilityResource eligibility(@PathVariable String id, @RequestParam(required = false) String subsystem) {
        CertificationScope scope = subsystem == null ? new CertificationScope.WholeAsset()
                : new CertificationScope.SubsystemScope(new SubsystemCode(subsystem));
        EligibilityResource eligibility = new EligibilityResource(
                evaluateCertification.execute(new EvaluateCertificationCommand(new InspectionId(id), scope)));
        eligibility.add(linkTo(methodOn(EligibilityController.class).eligibility(id, subsystem)).withSelfRel(),
                linkTo(methodOn(InspectionController.class).find(id)).withRel("inspection"));
        if (eligibility.eligible) {
            eligibility.add(Link.of("/api/v1/certificates").withRel("certificates"));
        }
        return eligibility;
    }
}
