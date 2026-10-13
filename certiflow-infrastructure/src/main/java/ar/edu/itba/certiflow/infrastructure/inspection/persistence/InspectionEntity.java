package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.SchemaVersionEntity;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.InspectionAssignment;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionScope;
import ar.edu.itba.certiflow.inspection.model.InspectionSnapshot;
import ar.edu.itba.certiflow.inspection.model.Revisions;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inspection")
public class InspectionEntity {

    @Id
    private String id;
    @Column(name = "asset_code")
    private String assetCode;
    @Column(name = "asset_responsible_id")
    private String assetResponsibleId;
    @Column(name = "asset_responsible_name")
    private String assetResponsibleName;
    @Column(name = "inspector_id")
    private String inspectorId;
    @Column(name = "inspector_name")
    private String inspectorName;
    @Column(name = "planned_date")
    private LocalDate plannedDate;
    @Column(name = "scope_sections")
    private String scopeSections;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "schema_version_id")
    private SchemaVersionEntity schemaVersion;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "inspection_id", nullable = false)
    @OrderColumn(name = "position")
    private List<CriterionResponseEntity> responses = new ArrayList<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "revision", joinColumns = @JoinColumn(name = "inspection_id"))
    @OrderBy("number")
    private List<RevisionEntity> revisions = new ArrayList<>();

    protected InspectionEntity() {
    }

    InspectionEntity(InspectionSnapshot snapshot, SchemaVersionEntity schemaVersion) {
        this.id = snapshot.id().value();
        this.schemaVersion = schemaVersion;
        copyFrom(snapshot);
    }

    void copyFrom(InspectionSnapshot snapshot) {
        PersonColumns responsible = PersonColumns.of(snapshot.assetResponsible());
        PersonColumns inspector = PersonColumns.of(snapshot.assignment().inspector());
        assetCode = snapshot.assetCode().value();
        assetResponsibleId = responsible.id();
        assetResponsibleName = responsible.name();
        inspectorId = inspector.id();
        inspectorName = inspector.name();
        plannedDate = snapshot.assignment().plannedDate();
        scopeSections = snapshot.assignment().scope().describedBy(new ScopeSectionsColumn()).orElse(null);
        List<CriterionResponse<?>> current = snapshot.responses();
        for (int index = 0; index < current.size(); index++) {
            if (index < responses.size()) {
                responses.get(index).copyFrom(current.get(index));
            } else {
                responses.add(CriterionResponseEntity.from(current.get(index)));
            }
        }
        revisions.clear();
        snapshot.revisions().asList().forEach(revision -> revisions.add(RevisionEntity.from(revision)));
    }

    InspectionSnapshot toSnapshot() {
        SchemaVersion version = schemaVersion.toDomain();
        InspectionScope scope = new StoredInspectionScope(scopeSections).scope();
        return new InspectionSnapshot(new InspectionId(id), new AssetCode(assetCode), new AssetType(schemaVersion.assetType()),
                new PersonColumns(assetResponsibleId, assetResponsibleName).toDomain(),
                new InspectionAssignment(new PersonColumns(inspectorId, inspectorName).toDomain(), plannedDate, scope),
                version,
                responses.stream().<CriterionResponse<?>>map(response -> response.toDomain(version)).toList(),
                new Revisions(revisions.stream().map(revision -> revision.toDomain(version)).toList()));
    }
}
