package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.domain.audit.AuditService;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public final class AuditedCertificateGenerator implements CertificateGenerator {

    private final AuditService auditService;

    public AuditedCertificateGenerator(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public Certificate generate(CertificateNumber number, Asset asset, Inspection basedOn, ValidityPeriod validity,
                                Person by, Instant at) {
        return audited(new StandardCertificate(number, asset, basedOn, validity, by, at), by, at);
    }

    Certificate audited(Certificate certificate, Person by, Instant at) {
        Certificate audited = new AuditedCertificate(certificate, auditService, this);
        auditService.record(new AuditEntry(audited, CertificateAudit.ISSUED, by, at,
                "Number " + certificate.number().value()));
        return audited;
    }
}
