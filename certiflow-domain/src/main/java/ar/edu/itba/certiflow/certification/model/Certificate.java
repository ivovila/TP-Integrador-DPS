package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateAlreadyRenewedException;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotActiveException;
import ar.edu.itba.certiflow.certification.model.exceptions.InspectionOfAnotherAssetException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.certification.model.exceptions.RenewalRequiresLaterInspectionException;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditTrail;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Certificate {

    private final CertificateNumber number;
    private final AssetCode assetCode;
    private final CertificateBasis basis;
    private final CertificationScope scope;
    private final SchemaReference schemaReference;
    private final Set<SubsystemCode> requiredSubsystems;
    private final ValidityPeriod validity;
    private final CertificationDecision decision;
    private final Person issuedBy;
    private final Instant issuedAt;
    private final List<Suspension> suspensions = new ArrayList<>();
    private final AuditTrail auditTrail = new AuditTrail();
    private CertificateLifecycle lifecycle = CertificateLifecycle.ISSUED;

    Certificate(CertificateNumber number, InspectionResult basedOn, CertificationDecision decision, Person issuedBy, Instant issuedAt) {
        this(number, basedOn.assetCode(), new CertificateBasis.InspectionBasis(basedOn.reference()), basedOn.scope(),
                basedOn.schemaReference(), basedOn.subsystems().stream().map(SubsystemResult::code).collect(Collectors.toSet()),
                decision, issuedBy, issuedAt);
    }

    Certificate(CertificateNumber number, AssetCode assetCode, CertificateBasis basis, CertificationScope scope,
            SchemaReference schemaReference, Set<SubsystemCode> requiredSubsystems,
            CertificationDecision decision, Person issuedBy, Instant issuedAt) {
        if (!decision.isEligible()) { throw new NotEligibleForCertificationException(decision); }
        if (!decision.scope().equals(scope)) { throw new CertificationConflictException("Decision scope must match certificate scope"); }
        this.number = number;
        this.assetCode = assetCode;
        this.basis = basis;
        this.scope = scope;
        this.schemaReference = schemaReference;
        this.requiredSubsystems = Set.copyOf(requiredSubsystems);
        this.decision = decision;
        this.validity = decision.validity().orElseThrow();
        this.issuedBy = issuedBy;
        this.issuedAt = issuedAt;
        auditTrail.record(CertificateAudit.ISSUED, issuedBy, issuedAt, "Number " + number.value() + ", scope " + scope
                + ", policy " + decision.policyCode() + ", jurisdiction " + decision.jurisdictionCode().value()
                + ", outcome " + decision.outcome() + ", validity " + validity + ", conditions " + decision.conditions());
    }

    Certificate(CertificateSnapshot snapshot) {
        this.number = snapshot.number();
        this.assetCode = snapshot.assetCode();
        this.basis = snapshot.basis();
        this.scope = snapshot.scope();
        this.schemaReference = snapshot.schemaReference();
        this.requiredSubsystems = snapshot.requiredSubsystems();
        this.validity = snapshot.validity();
        this.decision = snapshot.decision();
        this.issuedBy = snapshot.issuedBy();
        this.issuedAt = snapshot.issuedAt();
        this.suspensions.addAll(snapshot.suspensions());
        this.lifecycle = snapshot.lifecycle();
    }

    public CertificateSnapshot snapshot() {
        return new CertificateSnapshot(number, assetCode, basis, scope, schemaReference, requiredSubsystems, validity, issuedBy, issuedAt, suspensions(), lifecycle, decision);
    }

    void suspend(String reason, Person suspendedBy, Instant suspendedAt, LocalDate suspendedOn) {
        if (statusOn(suspendedOn) != CertificateStatus.ACTIVE) {
            throw new CertificateNotActiveException(number);
        }
        suspensions.add(new Suspension(reason, suspendedBy, suspendedAt));
        lifecycle = CertificateLifecycle.SUSPENDED;
        auditTrail.record(CertificateAudit.SUSPENDED, suspendedBy, suspendedAt, reason);
    }

    void ensureRenewableWith(InspectionResult newBasis) {
        if (lifecycle == CertificateLifecycle.RENEWED) {
            throw new CertificateAlreadyRenewedException(number);
        }
        if (!newBasis.assetCode().equals(assetCode)) {
            throw new InspectionOfAnotherAssetException(assetCode);
        }
        if (!newBasis.scope().equals(scope)) { throw new CertificationConflictException("Renewal scope must match"); }
        if (!newBasis.reference().closedAt().isAfter(latestInspectionClosedAt())) {
            throw new RenewalRequiresLaterInspectionException(number);
        }
    }

    Certificate renew(CertificateNumber newNumber, CertificationDecision newDecision, InspectionResult newBasis,
                             Person renewedBy, Instant renewedAt) {
        ensureRenewableWith(newBasis);
        Certificate renewedCertificate = new Certificate(newNumber, newBasis, newDecision, renewedBy, renewedAt);
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

    public CertificationDecision decision() { return decision; }

    public CertificateNumber number() {
        return number;
    }

    public AssetCode assetCode() {
        return assetCode;
    }

    public InspectionReference basedOn() {
        if (basis instanceof CertificateBasis.InspectionBasis inspection) { return inspection.inspection(); }
        throw new CertificationConflictException("Global certificate has partial certificate bases");
    }

    public CertificateBasis basis() { return basis; }
    public CertificationScope scope() { return scope; }
    public SchemaReference schemaReference() { return schemaReference; }
    public Set<SubsystemCode> requiredSubsystems() { return requiredSubsystems; }

    public Instant latestInspectionClosedAt() {
        return switch (basis) {
            case CertificateBasis.InspectionBasis inspection -> inspection.inspection().closedAt();
            case CertificateBasis.PartialCertificatesBasis partials -> partials.latestInspectionClosedAt();
        };
    }

    public CertificateStatus effectiveStatusOn(LocalDate date, List<Certificate> partials) {
        CertificateStatus own = statusOn(date);
        if (own == CertificateStatus.ACTIVE && basis instanceof CertificateBasis.PartialCertificatesBasis backing) {
            for (CertificateNumber number : backing.partials()) {
                if (partials.stream().noneMatch(partial -> partial.number().equals(number) && partial.statusOn(date) == CertificateStatus.ACTIVE)) {
                    return CertificateStatus.DEPENDENCY_INVALID;
                }
            }
        }
        return own;
    }

    void renewedBy(Certificate replacement, Person by, Instant at) {
        if (lifecycle == CertificateLifecycle.RENEWED) { throw new CertificateAlreadyRenewedException(number); }
        if (!assetCode.equals(replacement.assetCode()) || !scope.equals(replacement.scope())) {
            throw new CertificationConflictException("Renewal must retain asset and scope");
        }
        if (!replacement.latestInspectionClosedAt().isAfter(latestInspectionClosedAt())) {
            throw new RenewalRequiresLaterInspectionException(number);
        }
        lifecycle = CertificateLifecycle.RENEWED;
        auditTrail.record(CertificateAudit.RENEWED, by, at, "Renewed by " + replacement.number().value());
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
