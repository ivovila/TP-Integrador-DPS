package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditTrail;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateAlreadyRenewedException;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotActiveException;
import ar.edu.itba.certiflow.certification.model.exceptions.InspectionOfAnotherAssetException;
import ar.edu.itba.certiflow.certification.model.exceptions.RenewalRequiresLaterInspectionException;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class Certificate {

    private final CertificateNumber number;
    private final AssetCode assetCode;
    private final InspectionReference basedOn;
    private final ValidityPeriod validity;
    private final Person issuedBy;
    private final Instant issuedAt;
    private final List<Suspension> suspensions = new ArrayList<>();
    private final AuditTrail auditTrail = new AuditTrail();
    private CertificateLifecycle lifecycle = CertificateLifecycle.ISSUED;

    Certificate(CertificateNumber number, AssetCode assetCode, InspectionView basedOn, ValidityPeriod validity,
                        Person issuedBy, Instant issuedAt) {
        this.number = number;
        this.assetCode = assetCode;
        this.validity = validity;
        this.issuedBy = issuedBy;
        this.issuedAt = issuedAt;
        if (!basedOn.isClosed()) {
            throw new InspectionNotClosedException();
        }
        if (!basedOn.assetCode().equals(assetCode)) {
            throw new InspectionOfAnotherAssetException(assetCode);
        }
        this.basedOn = new InspectionReference(basedOn.id(), basedOn.originalRevision().sealedAt());
        auditTrail.record(CertificateAudit.ISSUED, issuedBy, issuedAt, "Number " + number.value());
    }

    void suspend(String reason, Person suspendedBy, Instant suspendedAt, LocalDate suspendedOn) {
        if (statusOn(suspendedOn) != CertificateStatus.ACTIVE) {
            throw new CertificateNotActiveException(number);
        }
        suspensions.add(new Suspension(reason, suspendedBy, suspendedAt));
        lifecycle = CertificateLifecycle.SUSPENDED;
        auditTrail.record(CertificateAudit.SUSPENDED, suspendedBy, suspendedAt, reason);
    }

    void ensureRenewableWith(InspectionView newBasis) {
        if (lifecycle == CertificateLifecycle.RENEWED) {
            throw new CertificateAlreadyRenewedException(number);
        }
        if (!newBasis.assetCode().equals(assetCode)) {
            throw new InspectionOfAnotherAssetException(assetCode);
        }
        if (!newBasis.originalRevision().sealedAt().isAfter(basedOn.closedAt())) {
            throw new RenewalRequiresLaterInspectionException(number);
        }
    }

    Certificate renew(CertificateNumber newNumber, ValidityPeriod newValidity, InspectionView newBasis,
                             Person renewedBy, Instant renewedAt) {
        ensureRenewableWith(newBasis);
        Certificate renewedCertificate = new Certificate(newNumber, assetCode, newBasis, newValidity, renewedBy, renewedAt);
        lifecycle = CertificateLifecycle.RENEWED;
        auditTrail.record(CertificateAudit.RENEWED, renewedBy, renewedAt, "Renewed by " + newNumber.value());
        return renewedCertificate;
    }

    List<AuditEntry> pullAuditEntries() {
        return auditTrail.pull();
    }

    public CertificateStatus statusOn(LocalDate referenceDate) {
        return lifecycle.statusOn(validity, referenceDate);
    }

    public boolean isCurrentOn(LocalDate referenceDate) {
        return lifecycle != CertificateLifecycle.RENEWED && validity.covers(referenceDate);
    }

    public boolean certifies(AssetCode candidate) {
        return assetCode.equals(candidate);
    }

    public CertificateNumber number() {
        return number;
    }

    public AssetCode assetCode() {
        return assetCode;
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
