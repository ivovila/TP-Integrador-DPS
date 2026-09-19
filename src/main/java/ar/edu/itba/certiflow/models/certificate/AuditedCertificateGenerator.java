package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditService;
import ar.edu.itba.certiflow.models.audit.AuditTrail;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public final class AuditedCertificateGenerator {

    private final AuditTrail auditTrail;

    public AuditedCertificateGenerator(AuditService auditService) {
        this.auditTrail = new AuditTrail(auditService);
    }

    public Certificate generate(CertificateNumber certificateNumber, Asset asset, InspectionView basedOn,
                                ValidityPeriod validity, Person issuedBy, Instant issuedAt) {
        Certificate auditedCertificate = new AuditedCertificate(new StandardCertificate(certificateNumber, asset, basedOn, validity, issuedBy, issuedAt),
                auditTrail);
        auditTrail.record(auditedCertificate, CertificateAudit.ISSUED, issuedBy, issuedAt, "Number " + certificateNumber.value());
        return auditedCertificate;
    }
}
