package ar.edu.itba.certiflow.application;

import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaCommand;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionCommand;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.SchemaAudit;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaAlreadyDefinedForAssetTypeException;
import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaVersioningTest {

    private final CertiflowFixture fixture = new CertiflowFixture();
    private final Criterion<YesNoAnswer> hoseCriterion = new Criterion<>("EXT-04", "Hose shows no cracks",
            new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of());

    @Test
    void newSchemaVersionDoesNotAffectAnInspectionAlreadyStarted() {
        InspectionId startedInspection = fixture.assignedInspection();

        fixture.publishSchemaVersion.execute(new PublishSchemaVersionCommand(CertiflowFixture.EXTINGUISHER, sectionsIncludingHose(), fixture.planner));

        assertEquals(1, fixture.inspection(startedInspection).schemaVersion().number());
        assertThrows(CriterionNotInSchemaException.class,
                () -> fixture.answer(startedInspection, hoseCriterion, YesNoAnswer.YES, fixture.inspector));
        fixture.answerAll(startedInspection, "7.00", YesNoAnswer.YES, "VISIBLE");
        assertDoesNotThrow(() -> fixture.close(startedInspection, fixture.inspector));
    }

    @Test
    void inspectionsAssignedAfterPublishingUseTheNewVersion() {
        fixture.publishSchemaVersion.execute(new PublishSchemaVersionCommand(CertiflowFixture.EXTINGUISHER, sectionsIncludingHose(), fixture.planner));

        InspectionId inspectionAssignedLater = fixture.assignedInspection();

        assertEquals(2, fixture.inspection(inspectionAssignedLater).schemaVersion().number());
        assertDoesNotThrow(() -> fixture.answer(inspectionAssignedLater, hoseCriterion, YesNoAnswer.YES, fixture.inspector));
    }

    @Test
    void publishingAVersionYieldsANewSchemaAndLeavesThePreviousOneUntouched() {
        InspectionSchema before = fixture.inspectionSchemaRepository.findByAssetType(CertiflowFixture.EXTINGUISHER).orElseThrow();

        fixture.publishSchemaVersion.execute(new PublishSchemaVersionCommand(CertiflowFixture.EXTINGUISHER, sectionsIncludingHose(), fixture.planner));

        InspectionSchema after = fixture.inspectionSchemaRepository.findByAssetType(CertiflowFixture.EXTINGUISHER).orElseThrow();
        assertEquals(1, before.versions().size());
        assertEquals(1, before.currentVersion().number());
        assertEquals(2, after.versions().size());
        assertEquals(2, after.currentVersion().number());
        assertEquals(before.versions().getFirst(), after.versions().getFirst());
    }

    @Test
    void creatingTheSchemaAndAddingVersionsIsAudited() {
        fixture.clock.advanceDays(1);
        fixture.publishSchemaVersion.execute(new PublishSchemaVersionCommand(CertiflowFixture.EXTINGUISHER, sectionsIncludingHose(), fixture.certifier));

        List<AuditEntry> history = fixture.schemaLog.historyOf(CertiflowFixture.EXTINGUISHER);
        assertEquals(List.of(SchemaAudit.CREATED, SchemaAudit.VERSION_ADDED), history.stream().map(AuditEntry::action).toList());
        assertEquals(fixture.planner, history.getFirst().performedBy());
        assertEquals(fixture.certifier, history.getLast().performedBy());
        assertEquals(fixture.clock.instant(), history.getLast().occurredAt());
        assertEquals("Version 2", history.getLast().detail());
    }

    @Test
    void assetTypeAcceptsASingleSchemaAndFurtherChangesArePublishedAsVersions() {
        assertThrows(SchemaAlreadyDefinedForAssetTypeException.class, () -> fixture.createInspectionSchema.execute(new CreateInspectionSchemaCommand("Another extinguisher schema", CertiflowFixture.EXTINGUISHER, fixture.firstVersionSections(), fixture.planner)));
    }

    @Test
    void versionCannotBePublishedForAnAssetTypeWithoutSchema() {
        assertThrows(NoInspectionSchemaForAssetTypeException.class,
                () -> fixture.publishSchemaVersion.execute(new PublishSchemaVersionCommand(new AssetType("Laboratory"), fixture.firstVersionSections(), fixture.planner)));
    }

    private List<Section> sectionsIncludingHose() {
        return List.of(new Section("General condition", List.of(fixture.pressureCriterion, fixture.sealIntactCriterion, fixture.signageCriterion, hoseCriterion)));
    }
}
