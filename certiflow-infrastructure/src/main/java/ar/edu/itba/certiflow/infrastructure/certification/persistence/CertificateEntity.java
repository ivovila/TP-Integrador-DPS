package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.CertificateBasis;
import ar.edu.itba.certiflow.certification.model.CertificateLifecycle;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateSnapshot;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.InspectionReference;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesJson;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "certificate")
public class CertificateEntity {

    @Id
    private String number;
    @Column(name = "asset_code")
    private String assetCode;
    @Column(name = "inspection_id")
    private String inspectionId;
    @Column(name = "inspection_closed_at")
    private Instant inspectionClosedAt;
    @Column(name = "valid_from")
    private LocalDate validFrom;
    @Column(name = "valid_until")
    private LocalDate validUntil;
    @Column(name = "issued_by_id")
    private String issuedById;
    @Column(name = "issued_by_name")
    private String issuedByName;
    @Column(name = "issued_at")
    private Instant issuedAt;
    @Enumerated(EnumType.STRING)
    private CertificateLifecycle lifecycle;
    private String decision;
    @Column(name = "scope_type") private String scopeType;
    @Column(name = "subsystem_code") private String subsystemCode;
    @Column(name = "basis_type") private String basisType;
    @Column(name = "schema_asset_type") private String schemaAssetType;
    @Column(name = "schema_number") private int schemaNumber;
    @Column(name = "required_subsystems") private String requiredSubsystems;
    @Column(name = "global_policy_code") private String globalPolicyCode;
    @Column(name = "latest_inspection_closed_at") private Instant latestInspectionClosedAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "certificate_backing", joinColumns = @JoinColumn(name = "global_number"))
    @Column(name = "partial_number")
    @OrderColumn(name = "position")
    private List<String> partials = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "suspension", joinColumns = @JoinColumn(name = "certificate_number"))
    @OrderColumn(name = "position")
    private List<SuspensionEmbeddable> suspensions = new ArrayList<>();

    protected CertificateEntity() {
    }

    static CertificateEntity from(CertificateSnapshot snapshot) {
        CertificateEntity entity = new CertificateEntity();
        entity.number = snapshot.number().value();
        entity.copyFrom(snapshot);
        return entity;
    }

    void copyFrom(CertificateSnapshot snapshot) {
        PersonColumns issuer = PersonColumns.of(snapshot.issuedBy());
        assetCode = snapshot.assetCode().value();
        scopeType = snapshot.scope() instanceof CertificationScope.WholeAsset ? "WHOLE" : "SUBSYSTEM";
        subsystemCode = snapshot.scope() instanceof CertificationScope.SubsystemScope subsystem ? subsystem.code().value() : null;
        schemaAssetType = snapshot.schemaReference().assetType().name();
        schemaNumber = snapshot.schemaReference().number();
        requiredSubsystems = CodesJson.write(snapshot.requiredSubsystems().stream().map(code -> code.value()).sorted().toList());
        partials.clear();
        if (snapshot.basis() instanceof CertificateBasis.InspectionBasis inspection) {
            basisType = "INSPECTION";
            inspectionId = inspection.inspection().id().value();
            inspectionClosedAt = inspection.inspection().closedAt();
        } else {
            basisType = "PARTIALS";
            var backing = (CertificateBasis.PartialCertificatesBasis) snapshot.basis();
            partials.addAll(backing.partials().stream().map(number -> number.value()).toList());
            globalPolicyCode = backing.globalPolicy();
            latestInspectionClosedAt = backing.latestInspectionClosedAt();
        }
        validFrom = snapshot.validity().validFrom();
        validUntil = snapshot.validity().validUntil();
        issuedById = issuer.id();
        issuedByName = issuer.name();
        issuedAt = snapshot.issuedAt();
        lifecycle = snapshot.lifecycle();
        decision = DecisionJson.write(snapshot.decision());
        suspensions.clear();
        snapshot.suspensions().forEach(suspension -> suspensions.add(SuspensionEmbeddable.of(suspension)));
    }

    private CertificateBasis basis() {
        if (basisType.equals("INSPECTION")) {
            return new CertificateBasis.InspectionBasis(new InspectionReference(new InspectionId(inspectionId), inspectionClosedAt));
        }
        return new CertificateBasis.PartialCertificatesBasis(
                partials.stream().map(CertificateNumber::new).toList(),
                globalPolicyCode, latestInspectionClosedAt);
    }

    CertificateSnapshot toSnapshot() {
        return new CertificateSnapshot(new CertificateNumber(number), new AssetCode(assetCode),
                basis(), scopeType.equals("WHOLE") ? new CertificationScope.WholeAsset()
                        : new CertificationScope.SubsystemScope(new SubsystemCode(subsystemCode)),
                new SchemaReference(new AssetType(schemaAssetType), schemaNumber),
                CodesJson.read(requiredSubsystems).stream().map(SubsystemCode::new).collect(Collectors.toSet()),
                new ValidityPeriod(validFrom, validUntil),
                new Stamp(new PersonColumns(issuedById, issuedByName).toDomain(), issuedAt),
                suspensions.stream().map(SuspensionEmbeddable::toDomain).toList(), lifecycle, DecisionJson.read(decision));
    }
}
