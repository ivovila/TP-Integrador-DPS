package ar.edu.itba.certiflow.web.certification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import ar.edu.itba.certiflow.web.support.Uris;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateStatus;
import ar.edu.itba.certiflow.certification.usecase.FindCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.GenerateCertificateDocumentUseCase;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.IssueCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.ListCertificatesUseCase;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.RenewCertificateUseCase;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateCommand;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateUseCase;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import ar.edu.itba.certiflow.shared.usecase.AuditSubject;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import ar.edu.itba.certiflow.web.support.AuditEntryResponse;
import ar.edu.itba.certiflow.web.support.PageLinks;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
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
    private final Clock clock;

    public CertificateController(IssueCertificateUseCase issueCertificate, ListCertificatesUseCase listCertificates,
                                 FindCertificateUseCase findCertificate, GenerateCertificateDocumentUseCase generateCertificateDocument,
                                 SuspendCertificateUseCase suspendCertificate, RenewCertificateUseCase renewCertificate,
                                 AuditHistoryUseCase auditHistory, Clock clock) {
        this.issueCertificate = issueCertificate;
        this.listCertificates = listCertificates;
        this.findCertificate = findCertificate;
        this.generateCertificateDocument = generateCertificateDocument;
        this.suspendCertificate = suspendCertificate;
        this.renewCertificate = renewCertificate;
        this.auditHistory = auditHistory;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<CertificateResource> issue(@RequestBody CertificateRequest request, @ActingPerson Person issuedBy) {
        CertificateNumber number = issueCertificate.execute(new IssueCertificateCommand(new InspectionId(request.inspectionId()),
                request.validUntil(), issuedBy));
        return created(number);
    }

    @GetMapping
    public PagedModel<CertificateResource> list(@RequestParam Optional<String> asset, @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return PageLinks.page(listCertificates.execute(asset.map(AssetCode::new)).stream().map(this::resource).toList(), page, size,
                "/api/v1/certificates", Uris.query("asset", asset).orElse(""));
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
    public ResponseEntity<CertificateResource> suspend(@PathVariable String number, @RequestBody SuspensionRequest request,
                                                       @ActingPerson Person suspendedBy) {
        suspendCertificate.execute(new SuspendCertificateCommand(new CertificateNumber(number), request.reason(), suspendedBy));
        return created(new CertificateNumber(number));
    }

    @PostMapping("/{number}/renewals")
    public ResponseEntity<CertificateResource> renew(@PathVariable String number, @RequestBody CertificateRequest request,
                                                     @ActingPerson Person renewedBy) {
        CertificateNumber renewed = renewCertificate.execute(new RenewCertificateCommand(new CertificateNumber(number),
                new InspectionId(request.inspectionId()), request.validUntil(), renewedBy));
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

    private CertificateResource resource(Certificate certificate) {
        String number = certificate.number().value();
        CertificateStatus status = certificate.statusOn(LocalDate.now(clock));
        CertificateResource resource = new CertificateResource(certificate, status);
        resource.add(linkTo(methodOn(CertificateController.class).find(number)).withSelfRel(),
                Link.of(Uris.path("assets", certificate.assetCode().value())).withRel("asset"),
                Link.of(Uris.path("inspections", certificate.basedOn().id().value())).withRel("inspection"),
                linkTo(methodOn(CertificateController.class).document(number)).withRel("document"),
                linkTo(methodOn(CertificateController.class).history(number)).withRel("audit-entries"));
        if (status == CertificateStatus.ACTIVE) {
            resource.add(linkTo(methodOn(CertificateController.class).suspend(number, null, null)).withRel("suspensions"));
        }
        if (status != CertificateStatus.RENEWED) {
            resource.add(linkTo(methodOn(CertificateController.class).renew(number, null, null)).withRel("renewals"));
        }
        return resource;
    }
}
