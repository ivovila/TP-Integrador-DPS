package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public final class AuditedCertificateGenerator {

    private final AuditLog<Certificate> auditLog;

    public AuditedCertificateGenerator(AuditLog<Certificate> auditLog) {
        this.auditLog = auditLog;
    }

    public Certificate generate(CertificateNumber certificateNumber, Asset asset, InspectionView basedOn,
                                ValidityPeriod validity, Person issuedBy, Instant issuedAt) {
        Certificate auditedCertificate = new AuditedCertificate(new StandardCertificate(certificateNumber, asset, basedOn, validity, issuedBy, issuedAt),
                auditLog);
        auditLog.record(auditedCertificate, new AuditEntry(CertificateAudit.ISSUED, issuedBy, issuedAt, "Number " + certificateNumber.value()));
        return auditedCertificate;
    }
}
