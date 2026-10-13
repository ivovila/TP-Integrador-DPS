package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.infrastructure.schema.persistence.SectionsDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.SubsystemColumn;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionSnapshot;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingSnapshot;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Entity
@Table(name = "finding")
public class FindingEntity {

    @Id
    private String id;
    @Column(name = "inspection_id")
    private String inspectionId;
    @Column(name = "asset_code")
    private String assetCode;
    private String criterion;
    @Column(name = "subsystem_code")
    private String subsystemCode;
    @Column(name = "responsible_id")
    private String responsibleId;
    @Column(name = "responsible_name")
    private String responsibleName;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "finding_evidence", joinColumns = @JoinColumn(name = "finding_id"))
    @OrderColumn(name = "position")
    private List<EvidenceEmbeddable> evidence = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "finding_id", referencedColumnName = "id", insertable = false, updatable = false)
    @OrderBy("number")
    private List<CorrectiveActionEntity> actions = new ArrayList<>();

    protected FindingEntity() {
    }

    FindingEntity(FindingSnapshot snapshot, SectionsDocumentMapper sectionsDocuments) {
        this.id = snapshot.id().value();
        this.inspectionId = snapshot.inspectionId().value();
        this.assetCode = snapshot.assetCode().value();
        this.criterion = sectionsDocuments.writeCriterion(snapshot.criterion());
        this.subsystemCode = new SubsystemColumn(snapshot.subsystem()).subsystemCode();
        PersonColumns responsible = new PersonColumns(snapshot.responsible());
        this.responsibleId = responsible.id();
        this.responsibleName = responsible.name();
        snapshot.evidence().forEach(attached -> evidence.add(new EvidenceEmbeddable(attached)));
        copyActionsFrom(snapshot);
    }

    /** Las acciones solo crecen: se actualizan las filas que ya existen y después se agregan las nuevas. */
    void copyActionsFrom(FindingSnapshot snapshot) {
        List<CorrectiveActionSnapshot> current = snapshot.actions();
        int storedActions = actions.size();
        IntStream.range(0, storedActions).forEach(index -> actions.get(index).copyFrom(current.get(index)));
        current.subList(storedActions, current.size())
                .forEach(addedAction -> actions.add(new CorrectiveActionEntity(id, addedAction)));
    }

    FindingSnapshot toSnapshot(SectionsDocumentMapper sectionsDocuments) {
        return new FindingSnapshot(new FindingId(id), new InspectionId(inspectionId), new AssetCode(assetCode),
                sectionsDocuments.readCriterion(criterion), evidence.stream().map(EvidenceEmbeddable::toDomain).toList(),
                new PersonColumns(responsibleId, responsibleName).toDomain(),
                actions.stream().map(CorrectiveActionEntity::toSnapshot).toList(),
                new SubsystemColumn(subsystemCode).association());
    }
}
