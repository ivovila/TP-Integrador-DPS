package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditService;
import ar.edu.itba.certiflow.domain.audit.AuditTrail;
import ar.edu.itba.certiflow.domain.inspection.InspectionView;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public final class AuditedCertificateGenerator {

    private final AuditTrail audit;

    public AuditedCertificateGenerator(AuditService auditService) {
        this.audit = new AuditTrail(auditService);
    }

    public Certificate generate(CertificateNumber number, Asset asset, InspectionView basedOn,
                                ValidityPeriod validity, Person by, Instant at) {
        Certificate audited = new AuditedCertificate(new StandardCertificate(number, asset, basedOn, validity, by, at),
                audit);
        audit.record(audited, CertificateAudit.ISSUED, by, at, "Number " + number.value());
        return audited;
    }
}
