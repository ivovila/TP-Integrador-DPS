package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateAlreadyRenewedException;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotActiveException;
import ar.edu.itba.certiflow.certification.model.exceptions.InspectionOfAnotherAssetException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotAGlobalCertificateException;
import ar.edu.itba.certiflow.certification.model.exceptions.RenewalRequiresLaterInspectionException;
import ar.edu.itba.certiflow.certification.model.exceptions.RenewalScopeMismatchException;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditTrail;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
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
    private final SchemaReference schemaReference;
    private final Set<SubsystemCode> requiredSubsystems;
    private final GrantedDecision decision;
    private final Stamp issued;
    private final List<Suspension> suspensions = new ArrayList<>();
    private final AuditTrail auditTrail = new AuditTrail();
    private CertificateLifecycle lifecycle = CertificateLifecycle.ISSUED;

    Certificate(CertificateNumber number, InspectionResult basedOn, GrantedDecision decision, Stamp issued) {
        this(number, basedOn.assetCode(), new InspectionBasis(basedOn.reference()),
                basedOn.schemaReference(), basedOn.subsystems().stream().map(SubsystemResult::code).collect(Collectors.toSet()),
                decision, issued);
    }

    Certificate(CertificateNumber number, AssetCode assetCode, CertificateBasis basis,
            SchemaReference schemaReference, Set<SubsystemCode> requiredSubsystems,
            GrantedDecision decision, Stamp issued) {
        this.number = number;
        this.assetCode = assetCode;
        this.basis = basis;
        this.schemaReference = schemaReference;
        this.requiredSubsystems = Set.copyOf(requiredSubsystems);
        this.decision = decision;
        this.issued = issued;
        auditTrail.record(CertificateAudit.ISSUED, issued, "Number " + number.value() + ", scope " + scope().describedBy(new ScopeLabel())
                + ", policy " + decision.policyCode() + ", jurisdiction " + decision.jurisdictionCode().value()
                + ", outcome " + decision.outcome() + ", validity " + validity() + ", conditions " + decision.conditions());
    }

    Certificate(CertificateSnapshot snapshot) {
        this.number = snapshot.number();
        this.assetCode = snapshot.assetCode();
        this.basis = snapshot.basis();
        this.schemaReference = snapshot.schemaReference();
        this.requiredSubsystems = snapshot.requiredSubsystems();
        this.decision = snapshot.decision();
        this.issued = snapshot.issued();
        this.suspensions.addAll(snapshot.suspensions());
        this.lifecycle = snapshot.lifecycle();
    }

    public CertificateSnapshot snapshot() {
        return new CertificateSnapshot(number, assetCode, basis, schemaReference, requiredSubsystems, issued, suspensions(), lifecycle,
                decision);
    }

    void suspend(String reason, Stamp suspended, LocalDate suspendedOn) {
        if (!lifecycleStatusOn(suspendedOn).isActive()) {
            throw new CertificateNotActiveException(number);
        }
        suspensions.add(new Suspension(reason, suspended));
        lifecycle = CertificateLifecycle.SUSPENDED;
        auditTrail.record(CertificateAudit.SUSPENDED, suspended, reason);
    }

    void ensureRenewableWith(InspectionResult newBasis) {
        ensureReplaceableBy(newBasis.assetCode(), newBasis.scope(), newBasis.reference().closedAt());
    }

    /**
     * Las reglas para reemplazar este certificado, al renovar un parcial o un global: no puede estar ya renovado,
     * y el reemplazo tiene que ser del mismo activo y alcance y apoyarse en una inspección posterior.
     */
    void ensureReplaceableBy(AssetCode replacementAsset, CertificationScope replacementScope,
                             Instant replacementInspectionClosedAt) {
        if (lifecycle.isReplaced()) {
            throw new CertificateAlreadyRenewedException(number);
        }
        if (!assetCode.equals(replacementAsset)) {
            throw new InspectionOfAnotherAssetException(assetCode);
        }
        if (!scope().equals(replacementScope)) {
            throw new RenewalScopeMismatchException();
        }
        if (!replacementInspectionClosedAt.isAfter(latestInspectionClosedAt())) {
            throw new RenewalRequiresLaterInspectionException(number);
        }
    }

    List<AuditEntry> pullAuditEntries() {
        return auditTrail.pull();
    }

    private CertificateStatus lifecycleStatusOn(LocalDate referenceDate) {
        return lifecycle.statusOn(validity(), referenceDate);
    }

    /** Ocupa ese alcance en esa fecha: vigente y no renovado, aunque esté suspendido. */
    public boolean isCurrentFor(CertificationScope candidate, LocalDate referenceDate) {
        return scope().equals(candidate) && isCurrentOn(referenceDate);
    }

    public boolean isCurrentOn(LocalDate referenceDate) {
        return !lifecycle.isReplaced() && validity().covers(referenceDate);
    }

    public boolean certifies(AssetCode candidate) {
        return assetCode.equals(candidate);
    }

    public boolean isIssuedFor(SchemaReference candidate) {
        return schemaReference.equals(candidate);
    }

    public boolean wasDecidedUnder(JurisdictionCode candidate) {
        return decision.jurisdictionCode().equals(candidate);
    }

    public GrantedDecision decision() {
        return decision;
    }

    public CertificateNumber number() {
        return number;
    }

    public AssetCode assetCode() {
        return assetCode;
    }

    public CertificateBasis basis() { return basis; }
    public CertificationScope scope() {
        return decision.scope();
    }
    public SchemaReference schemaReference() { return schemaReference; }
    public Set<SubsystemCode> requiredSubsystems() { return requiredSubsystems; }

    public Instant latestInspectionClosedAt() {
        return basis.latestInspectionClosedAt();
    }

    /**
     * El estado del certificado en una fecha: el de su ciclo de vida (emitido, suspendido, renovado) y su vigencia.
     * Un global activo queda {@link CertificateStatus#DEPENDENCY_INVALID} si alguno de sus parciales, que se buscan
     * entre los certificados del activo, ya no está activo.
     */
    public CertificateStatus statusOn(LocalDate date, List<Certificate> assetCertificates) {
        CertificateStatus lifecycleStatus = lifecycleStatusOn(date);
        return basis.isSupportedOn(date, assetCertificates) ? lifecycleStatus : lifecycleStatus.withoutActiveBacking();
    }

    public boolean hasNumber(CertificateNumber candidate) {
        return number.equals(candidate);
    }

    public List<SubsystemCode> certifiedSubsystems() {
        return scope().describedBy(new CertifiedSubsystems());
    }

    public boolean certifiesOneOf(Set<SubsystemCode> candidateSubsystems) {
        return certifiedSubsystems().stream().anyMatch(candidateSubsystems::contains);
    }

    /** Un global solo reemplaza a otro global, y con las mismas reglas que cualquier reemplazo. */
    void ensureReplaceableByGlobal(DecidedGlobalCertification replacement) {
        if (!basis.backsGlobalCertificate()) {
            throw new NotAGlobalCertificateException(number);
        }
        ensureReplaceableBy(replacement.assetCode(), replacement.scope(), replacement.latestInspectionClosedAt());
    }

    void renewedBy(Certificate replacement, Stamp renewed) {
        ensureReplaceableBy(replacement.assetCode(), replacement.scope(), replacement.latestInspectionClosedAt());
        lifecycle = CertificateLifecycle.RENEWED;
        auditTrail.record(CertificateAudit.RENEWED, renewed, "Renewed by " + replacement.number().value());
    }

    public ValidityPeriod validity() {
        return decision.validity();
    }

    public Person issuedBy() {
        return issued.by();
    }

    public Instant issuedAt() {
        return issued.at();
    }

    public List<Suspension> suspensions() {
        return List.copyOf(suspensions);
    }
}
