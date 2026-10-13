package ar.edu.itba.certiflow.web.certification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateBasis;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateStatus;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.usecase.FindCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.GenerateCertificateDocumentUseCase;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.IssueGlobalCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.ListCertificatesUseCase;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.RenewGlobalCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewGlobalCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateUseCase;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import ar.edu.itba.certiflow.shared.usecase.AuditSubject;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.AuditEntryResponse;
import ar.edu.itba.certiflow.web.support.PageLinks;
import ar.edu.itba.certiflow.web.support.RequestFields;
import ar.edu.itba.certiflow.web.support.ResourceNotFoundException;
import ar.edu.itba.certiflow.web.support.Uris;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import ar.edu.itba.certiflow.certification.usecase.CertificateView;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final IssueCertificateUseCase issueCertificate;
    private final ListCertificatesUseCase listCertificates;
    private final FindCertificateUseCase findCertificate;
    private final GenerateCertificateDocumentUseCase generateCertificateDocument;
    private final SuspendCertificateUseCase suspendCertificate;
    private final RenewCertificateUseCase renewCertificate;
    private final AuditHistoryUseCase auditHistory;
    private final IssueGlobalCertificateUseCase issueGlobal;
    private final RenewGlobalCertificateUseCase renewGlobal;

    public CertificateController(IssueCertificateUseCase issueCertificate, ListCertificatesUseCase listCertificates,
                                 FindCertificateUseCase findCertificate, GenerateCertificateDocumentUseCase generateCertificateDocument,
                                 SuspendCertificateUseCase suspendCertificate, RenewCertificateUseCase renewCertificate,
                                 AuditHistoryUseCase auditHistory,
                                 IssueGlobalCertificateUseCase issueGlobal,
                                 RenewGlobalCertificateUseCase renewGlobal) {
        this.issueCertificate = issueCertificate;
        this.listCertificates = listCertificates;
        this.findCertificate = findCertificate;
        this.generateCertificateDocument = generateCertificateDocument;
        this.suspendCertificate = suspendCertificate;
        this.renewCertificate = renewCertificate;
        this.auditHistory = auditHistory;
        this.issueGlobal = issueGlobal;
        this.renewGlobal = renewGlobal;
    }

    @PostMapping
    public ResponseEntity<CertificateResource> issue(@RequestBody CertificateRequest request, @ActingPerson Person issuedBy) {
        CertificateNumber number = issueCertificate.execute(new IssueCertificateCommand(new InspectionId(request.inspectionId()),
                issuedBy, request.scope()));
        return created(number);
    }

    public record GlobalRequest(int schemaVersion, List<String> partialCertificateNumbers) {
        public GlobalRequest { RequestFields.requireElements(partialCertificateNumbers, "partialCertificateNumbers"); }
    }

    @PostMapping("/{number}/global-renewals")
    public ResponseEntity<CertificateResource> renewGlobal(@PathVariable String number, @RequestBody GlobalRequest request, @ActingPerson Person by) {
        return created(renewGlobal.execute(new RenewGlobalCertificateCommand(
                new CertificateNumber(number), findCertificate.execute(new CertificateNumber(number)).certificate().assetCode(), request.schemaVersion(), request.partialCertificateNumbers().stream().map(CertificateNumber::new).toList(), by)));
    }

    @GetMapping
    public PagedModel<CertificateResource> list(@RequestParam(required = false) String asset,
                                                @RequestParam(required = false) String subsystem, @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        List<CertificateView> certificates = Optional.ofNullable(asset).map(code -> listCertificates.execute(new AssetCode(code)))
                .orElseGet(listCertificates::execute);
        return PageLinks.page(certificates.stream()
                        .filter(view -> Optional.ofNullable(subsystem)
                                .map(code -> view.certificate().scope().equals(new CertificationScope.SubsystemScope(new SubsystemCode(code))))
                                .orElse(true))
                        .map(this::resource).toList(), page, size,
                "/api/v1/certificates", Stream.of(Uris.query("asset", asset).orElse(""), Uris.query("subsystem", subsystem).orElse("")).filter(value -> !value.isEmpty()).collect(Collectors.joining("&")));
    }

    @GetMapping("/{number}")
    public CertificateResource find(@PathVariable String number) {
        return resource(findCertificate.execute(new CertificateNumber(number)));
    }

    @GetMapping("/{number}/document")
    public CertificateDocumentResource document(@PathVariable String number) {
        CertificateDocumentResource document = new CertificateDocumentResource(
                generateCertificateDocument.execute(new CertificateNumber(number)));
        document.add(linkTo(methodOn(CertificateController.class).document(number)).withSelfRel(),
                linkTo(methodOn(CertificateController.class).find(number)).withRel("certificate"));
        return document;
    }

    @PostMapping("/{number}/suspensions")
    public ResponseEntity<EntityModel<CertificateResource.SuspensionView>> suspend(@PathVariable String number,
                                                                                   @RequestBody SuspensionRequest request,
                                                                                   @ActingPerson Person suspendedBy) {
        suspendCertificate.execute(new SuspendCertificateCommand(new CertificateNumber(number), request.reason(), suspendedBy));
        EntityModel<CertificateResource.SuspensionView> suspension = suspension(number, find(number).suspensions.size());
        return ResponseEntity.created(suspension.getRequiredLink("self").toUri()).body(suspension);
    }

    @GetMapping("/{number}/suspensions/{position}")
    public EntityModel<CertificateResource.SuspensionView> suspension(@PathVariable String number, @PathVariable int position) {
        CertificateResource certificate = find(number);
        if (position < 1 || position > certificate.suspensions.size()) {
            throw new ResourceNotFoundException("Certificate " + number + " has no suspension " + position);
        }
        return EntityModel.of(certificate.suspensions.get(position - 1),
                linkTo(methodOn(CertificateController.class).suspension(number, position)).withSelfRel(),
                linkTo(methodOn(CertificateController.class).find(number)).withRel("certificate"));
    }

    @PostMapping("/{number}/renewals")
    public ResponseEntity<CertificateResource> renew(@PathVariable String number, @RequestBody CertificateRequest request,
                                                     @ActingPerson Person renewedBy) {
        CertificateNumber current = new CertificateNumber(number);
        InspectionId inspection = new InspectionId(request.inspectionId());
        CertificateNumber renewed = renewCertificate.execute(request.requestedScope()
                .map(scope -> new RenewCertificateCommand(current, inspection, renewedBy, scope))
                .orElseGet(() -> new RenewCertificateCommand(current, inspection, renewedBy)));
        return created(renewed);
    }

    @GetMapping("/{number}/audit-entries")
    public CollectionModel<AuditEntryResponse> history(@PathVariable String number) {
        return CollectionModel.of(auditHistory.execute(new AuditSubject.OfCertificate(new CertificateNumber(number))).stream()
                        .map(AuditEntryResponse::of).toList(),
                linkTo(methodOn(CertificateController.class).history(number)).withSelfRel());
    }

    private ResponseEntity<CertificateResource> created(CertificateNumber number) {
        CertificateResource resource = find(number.value());
        return ResponseEntity.created(resource.getRequiredLink("self").toUri()).body(resource);
    }

    private CertificateResource resource(CertificateView view) {
        Certificate certificate = view.certificate();
        String number = certificate.number().value();
        CertificateStatus status = view.status();
        CertificateResource resource = new CertificateResource(certificate, status);
        resource.add(linkTo(methodOn(CertificateController.class).find(number)).withSelfRel(),
                Link.of(Uris.path("assets", certificate.assetCode().value())).withRel("asset"),
                linkTo(methodOn(CertificateController.class).document(number)).withRel("document"),
                linkTo(methodOn(CertificateController.class).history(number)).withRel("audit-entries"));
        if (certificate.basis() instanceof CertificateBasis.InspectionBasis inspection) {
            resource.add(Link.of(Uris.path("inspections", inspection.inspection().id().value())).withRel("inspection"));
        } else {
            var backing = (CertificateBasis.PartialCertificatesBasis) certificate.basis();
            backing.partials().forEach(partial -> resource.add(Link.of(Uris.path("certificates", partial.value())).withRel("partial-certificates")));
        }
        if (status == CertificateStatus.ACTIVE) {
            resource.add(linkTo(methodOn(CertificateController.class).suspend(number, null, null)).withRel("suspensions"));
        }
        if (status != CertificateStatus.RENEWED) {
            if (certificate.basis() instanceof CertificateBasis.PartialCertificatesBasis) {
                resource.add(linkTo(methodOn(CertificateController.class).renewGlobal(number, null, null)).withRel("global-renewals"));
            } else {
                resource.add(linkTo(methodOn(CertificateController.class).renew(number, null, null)).withRel("renewals"));
            }
        }
        return resource;
    }
}
