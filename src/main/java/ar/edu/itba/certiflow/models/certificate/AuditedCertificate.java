package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditTrail;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

final class AuditedCertificate implements Certificate {

    private final Certificate certificate;
    private final AuditTrail auditTrail;

    AuditedCertificate(Certificate certificate, AuditTrail auditTrail) {
        this.certificate = certificate;
        this.auditTrail = auditTrail;
    }

    @Override
    public void suspend(String reason, Person suspendedBy, Instant suspendedAt) {
        certificate.suspend(reason, suspendedBy, suspendedAt);
        auditTrail.record(this, CertificateAudit.SUSPENDED, suspendedBy, suspendedAt, reason);
    }

    @Override
    public Certificate renew(CertificateNumber renewalNumber, ValidityPeriod renewalValidity, InspectionView basedOn, Person renewedBy,
                             Instant renewedAt) {
        Certificate standardRenewal = certificate.renew(renewalNumber, renewalValidity, basedOn, renewedBy, renewedAt);
        auditTrail.record(this, CertificateAudit.RENEWED, renewedBy, renewedAt, "Renewed by " + renewalNumber.value());
        Certificate auditedRenewal = new AuditedCertificate(standardRenewal, auditTrail);
        auditTrail.record(auditedRenewal, CertificateAudit.ISSUED, renewedBy, renewedAt, "Number " + renewalNumber.value());
        return auditedRenewal;
    }

    @Override
    public CertificateStatus statusOn(LocalDate referenceDate) {
        return certificate.statusOn(referenceDate);
    }

    @Override
    public boolean isCurrentOn(LocalDate referenceDate) {
        return certificate.isCurrentOn(referenceDate);
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
    public InspectionView basedOn() {
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
