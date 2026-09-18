package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditTrail;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

final class AuditedCertificate implements Certificate {

    private final Certificate certificate;
    private final AuditTrail audit;

    AuditedCertificate(Certificate certificate, AuditTrail audit) {
        this.certificate = certificate;
        this.audit = audit;
    }

    @Override
    public void suspend(String reason, Person by, Instant at) {
        certificate.suspend(reason, by, at);
        audit.record(this, CertificateAudit.SUSPENDED, by, at, reason);
    }

    @Override
    public Certificate renew(CertificateNumber number, ValidityPeriod validity, Inspection basedOn, Person by,
                             Instant at) {
        Certificate rawRenewal = certificate.renew(number, validity, basedOn, by, at);
        audit.record(this, CertificateAudit.RENEWED, by, at, "Renewed by " + number.value());
        Certificate renewal = new AuditedCertificate(rawRenewal, audit);
        audit.record(renewal, CertificateAudit.ISSUED, by, at, "Number " + number.value());
        return renewal;
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
