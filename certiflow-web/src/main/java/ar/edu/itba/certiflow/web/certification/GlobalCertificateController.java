package ar.edu.itba.certiflow.web.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.usecase.*;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.web.support.ActingPerson;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assets/{code}/global-certificates")
public class GlobalCertificateController {
    private final IssueGlobalCertificateUseCase issue;
    private final CertificateController certificates;
    public GlobalCertificateController(IssueGlobalCertificateUseCase issue, CertificateController certificates) {
        this.issue = issue; this.certificates = certificates;
    }

    @PostMapping
    public ResponseEntity<CertificateResource> issue(@PathVariable String code,
            @RequestBody CertificateController.GlobalRequest request, @ActingPerson Person by) {
        var number = issue.execute(new IssueGlobalCertificateCommand(new AssetCode(code), request.schemaVersion(),
                request.partialCertificateNumbers().stream().map(CertificateNumber::new).toList(), by));
        var resource = certificates.find(number.value());
        return ResponseEntity.created(resource.getRequiredLink("self").toUri()).body(resource);
    }
}
