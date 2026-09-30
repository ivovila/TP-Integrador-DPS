package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditTrail;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateAlreadyRenewedException;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateNotActiveException;
import ar.edu.itba.certiflow.models.certificate.exceptions.InspectionOfAnotherAssetException;
import ar.edu.itba.certiflow.models.certificate.exceptions.RenewalRequiresLaterInspectionException;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class Certificate {

    private final CertificateNumber number;
    private final Asset asset;
    private final InspectionReference basedOn;
    private final ValidityPeriod validity;
    private final Person issuedBy;
    private final Instant issuedAt;
    private final List<Suspension> suspensions = new ArrayList<>();
    private final AuditTrail auditTrail = new AuditTrail();
    private CertificateLifecycle lifecycle = CertificateLifecycle.ISSUED;

    Certificate(CertificateNumber number, Asset asset, InspectionView basedOn, ValidityPeriod validity,
                        Person issuedBy, Instant issuedAt) {
        this.number = number;
        this.asset = asset;
        this.validity = validity;
        this.issuedBy = issuedBy;
        this.issuedAt = issuedAt;
        if (!basedOn.isClosed()) {
            throw new InspectionNotClosedException();
        }
        if (!basedOn.asset().equals(asset)) {
            throw new InspectionOfAnotherAssetException(asset);
        }
        this.basedOn = new InspectionReference(basedOn.id(), basedOn.originalRevision().sealedAt());
        auditTrail.record(CertificateAudit.ISSUED, issuedBy, issuedAt, "Number " + number.value());
    }

    public void suspend(String reason, Person suspendedBy, Instant suspendedAt, LocalDate suspendedOn) {
        if (statusOn(suspendedOn) != CertificateStatus.ACTIVE) {
            throw new CertificateNotActiveException(number);
        }
        suspensions.add(new Suspension(reason, suspendedBy, suspendedAt));
        lifecycle = CertificateLifecycle.SUSPENDED;
        auditTrail.record(CertificateAudit.SUSPENDED, suspendedBy, suspendedAt, reason);
    }

    public void ensureRenewableWith(InspectionView newBasis) {
        if (lifecycle == CertificateLifecycle.RENEWED) {
            throw new CertificateAlreadyRenewedException(number);
        }
        if (!newBasis.originalRevision().sealedAt().isAfter(basedOn.closedAt())) {
            throw new RenewalRequiresLaterInspectionException(number);
        }
    }

    public Certificate renew(CertificateNumber newNumber, ValidityPeriod newValidity, InspectionView newBasis,
                             Person renewedBy, Instant renewedAt) {
        ensureRenewableWith(newBasis);
        Certificate renewedCertificate = new Certificate(newNumber, asset, newBasis, newValidity, renewedBy, renewedAt);
        lifecycle = CertificateLifecycle.RENEWED;
        auditTrail.record(CertificateAudit.RENEWED, renewedBy, renewedAt, "Renewed by " + newNumber.value());
        return renewedCertificate;
    }

    public List<AuditEntry> pullAuditEntries() {
        return auditTrail.pull();
    }

    public CertificateStatus statusOn(LocalDate referenceDate) {
        return lifecycle.statusOn(validity, referenceDate);
    }

    public boolean isCurrentOn(LocalDate referenceDate) {
        return lifecycle != CertificateLifecycle.RENEWED && validity.covers(referenceDate);
    }

    public boolean certifies(Asset candidate) {
        return asset.equals(candidate);
    }

    public CertificateNumber number() {
        return number;
    }

    public Asset asset() {
        return asset;
    }

    public InspectionReference basedOn() {
        return basedOn;
    }

    public ValidityPeriod validity() {
        return validity;
    }

    public Person issuedBy() {
        return issuedBy;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public List<Suspension> suspensions() {
        return List.copyOf(suspensions);
    }
}
