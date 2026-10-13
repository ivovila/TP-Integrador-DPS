package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.certification.model.CertificateBasis;
import ar.edu.itba.certiflow.certification.model.CertificateBasisDescriber;
import ar.edu.itba.certiflow.certification.model.CertificateLifecycle;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateSnapshot;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.InspectionBasis;
import ar.edu.itba.certiflow.certification.model.InspectionReference;
import ar.edu.itba.certiflow.certification.model.PartialCertificatesBasis;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.UnreadableStoredDataException;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Stamp;
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
import java.util.stream.Stream;

@Entity
@Table(name = "certificate")
public class CertificateEntity {

    private static final String INSPECTION_BASIS = "INSPECTION";
    private static final String PARTIAL_CERTIFICATES_BASIS = "PARTIALS";

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

    CertificateEntity(CertificateSnapshot snapshot, DecisionDocumentMapper decisionDocuments,
                      CodesDocumentMapper codeDocuments) {
        this.number = snapshot.number().value();
        copyFrom(snapshot, decisionDocuments, codeDocuments);
    }

    void copyFrom(CertificateSnapshot snapshot, DecisionDocumentMapper decisionDocuments,
                  CodesDocumentMapper codeDocuments) {
        PersonColumns issuer = new PersonColumns(snapshot.issuedBy());
        assetCode = snapshot.assetCode().value();
        CertificationScope scope = snapshot.decision().scope();
        scopeType = scope.describedBy(new ScopeTypeColumn());
        subsystemCode = scope.describedBy(new ScopeSubsystemColumn()).orElse(null);
        schemaAssetType = snapshot.schemaReference().assetType().name();
        schemaNumber = snapshot.schemaReference().number();
        requiredSubsystems = codeDocuments.writeSubsystemCodes(snapshot.requiredSubsystems());
        partials.clear();
        basisType = snapshot.basis().describedBy(new BasisColumns());
        validFrom = snapshot.decision().validity().validFrom();
        validUntil = snapshot.decision().validity().validUntil();
        issuedById = issuer.id();
        issuedByName = issuer.name();
        issuedAt = snapshot.issuedAt();
        lifecycle = snapshot.lifecycle();
        decision = decisionDocuments.write(snapshot.decision());
        suspensions.clear();
        snapshot.suspensions().forEach(suspension -> suspensions.add(new SuspensionEmbeddable(suspension)));
    }

    private CertificateBasis basis() {
        return Stream.of(new StoredInspectionBasis(), new StoredPartialCertificatesBasis())
                .filter(storedBasis -> storedBasis.isStoredAs(basisType))
                .findFirst()
                .orElseThrow(() -> new UnreadableStoredDataException("certificate basis type", basisType))
                .basis();
    }

    /** Escribe las columnas de la base que le describen y devuelve su etiqueta. */
    private final class BasisColumns implements CertificateBasisDescriber<String> {

        @Override
        public String inspection(InspectionReference inspection) {
            inspectionId = inspection.id().value();
            inspectionClosedAt = inspection.closedAt();
            return INSPECTION_BASIS;
        }

        @Override
        public String partialCertificates(List<CertificateNumber> partialCertificateNumbers, String policyCode,
                                          Instant closedAt) {
            partials.addAll(partialCertificateNumbers.stream().map(CertificateNumber::value).toList());
            globalPolicyCode = policyCode;
            latestInspectionClosedAt = closedAt;
            return PARTIAL_CERTIFICATES_BASIS;
        }
    }

    private interface StoredBasis {

        boolean isStoredAs(String storedBasisType);

        CertificateBasis basis();
    }

    private final class StoredInspectionBasis implements StoredBasis {

        @Override
        public boolean isStoredAs(String storedBasisType) {
            return INSPECTION_BASIS.equals(storedBasisType);
        }

        @Override
        public CertificateBasis basis() {
            return new InspectionBasis(new InspectionReference(new InspectionId(inspectionId), inspectionClosedAt));
        }
    }

    private final class StoredPartialCertificatesBasis implements StoredBasis {

        @Override
        public boolean isStoredAs(String storedBasisType) {
            return PARTIAL_CERTIFICATES_BASIS.equals(storedBasisType);
        }

        @Override
        public CertificateBasis basis() {
            return new PartialCertificatesBasis(partials.stream().map(CertificateNumber::new).toList(),
                    globalPolicyCode, latestInspectionClosedAt);
        }
    }

    CertificateSnapshot toSnapshot(DecisionDocumentMapper decisionDocuments, CodesDocumentMapper codeDocuments) {
        return new CertificateSnapshot(new CertificateNumber(number), new AssetCode(assetCode),
                basis(),
                new SchemaReference(new AssetType(schemaAssetType), schemaNumber),
                codeDocuments.readSubsystemCodes(requiredSubsystems),
                new Stamp(new PersonColumns(issuedById, issuedByName).toDomain(), issuedAt),
                suspensions.stream().map(SuspensionEmbeddable::toDomain).toList(), lifecycle,
                decisionDocuments.read(decision));
    }
}
