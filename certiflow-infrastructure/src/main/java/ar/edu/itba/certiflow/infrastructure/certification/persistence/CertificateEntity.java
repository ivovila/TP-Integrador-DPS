package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.CertificateLifecycle;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateSnapshot;
import ar.edu.itba.certiflow.certification.model.InspectionReference;
import ar.edu.itba.certiflow.certification.model.ValidityPeriod;
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
        inspectionId = snapshot.basedOn().id().value();
        inspectionClosedAt = snapshot.basedOn().closedAt();
        validFrom = snapshot.validity().validFrom();
        validUntil = snapshot.validity().validUntil();
        issuedById = issuer.id();
        issuedByName = issuer.name();
        issuedAt = snapshot.issuedAt();
        lifecycle = snapshot.lifecycle();
        suspensions.clear();
        snapshot.suspensions().forEach(suspension -> suspensions.add(SuspensionEmbeddable.of(suspension)));
    }

    CertificateSnapshot toSnapshot() {
        return new CertificateSnapshot(new CertificateNumber(number), new AssetCode(assetCode),
                new InspectionReference(new InspectionId(inspectionId), inspectionClosedAt), new ValidityPeriod(validFrom, validUntil),
                new PersonColumns(issuedById, issuedByName).toDomain(), issuedAt,
                suspensions.stream().map(SuspensionEmbeddable::toDomain).toList(), lifecycle);
    }
}
