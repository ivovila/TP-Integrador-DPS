package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.domain.audit.AuditService;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

final class AuditedCertificate implements Certificate {

    private final Certificate certificate;
    private final AuditService auditService;
    private final AuditedCertificateGenerator generator;

    AuditedCertificate(Certificate certificate, AuditService auditService, AuditedCertificateGenerator generator) {
        this.certificate = certificate;
        this.auditService = auditService;
        this.generator = generator;
    }

    @Override
    public void suspend(String reason, Person by, Instant at) {
        certificate.suspend(reason, by, at);
        auditService.record(new AuditEntry(this, CertificateAudit.SUSPENDED, by, at, reason));
    }

    /** La renovación nace de este certificado y no del generador, así que se decora acá para que también quede auditada. */
    @Override
    public Certificate renew(CertificateNumber number, ValidityPeriod validity, Inspection basedOn, Person by,
                             Instant at) {
        Certificate renewal = certificate.renew(number, validity, basedOn, by, at);
        auditService.record(new AuditEntry(this, CertificateAudit.RENEWED, by, at, "Renewed by " + number.value()));
        return generator.audited(renewal, by, at);
    }

    @Override
    public CertificateStatus statusOn(LocalDate date) {
        return certificate.statusOn(date);
    }

    @Override
    public boolean isCurrentOn(LocalDate date) {
        return certificate.isCurrentOn(date);
    }

    @Override
    public boolean certifies(Asset asset) {
        return certificate.certifies(asset);
    }

    @Override
    public CertificateNumber number() {
        return certificate.number();
    }

    @Override
    public Asset asset() {
        return certificate.asset();
    }

    @Override
    public Inspection basedOn() {
        return certificate.basedOn();
    }

    @Override
    public ValidityPeriod validity() {
        return certificate.validity();
    }

    @Override
    public Person issuedBy() {
        return certificate.issuedBy();
    }

    @Override
    public Instant issuedAt() {
        return certificate.issuedAt();
    }

    @Override
    public List<Suspension> suspensions() {
        return certificate.suspensions();
    }
}
