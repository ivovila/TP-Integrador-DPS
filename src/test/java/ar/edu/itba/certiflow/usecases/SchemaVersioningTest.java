package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.application.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.application.exceptions.SchemaAlreadyDefinedForAssetTypeException;
import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.evaluation.StandardSeverity;
import ar.edu.itba.certiflow.models.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.models.evaluation.YesNoRule;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.models.schema.Section;
import ar.edu.itba.certiflow.models.schema.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaVersioningTest {

    private final CertiflowFixture fixture = new CertiflowFixture();
    private final Criterion<YesNoAnswer> hoseCriterion = new Criterion<>("EXT-04", "Hose shows no cracks",
            new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of());

    @Test
    void newSchemaVersionDoesNotAffectAnInspectionAlreadyStarted() {
        InspectionId startedInspection = fixture.assignedInspection();

        fixture.publishSchemaVersion.execute(CertiflowFixture.EXTINGUISHER, sectionsIncludingHose());

        assertEquals(1, fixture.inspection(startedInspection).schemaVersion().number());
        assertThrows(CriterionNotInSchemaException.class,
                () -> fixture.recordAnswer.execute(startedInspection, hoseCriterion, YesNoAnswer.YES, fixture.inspector));
        fixture.answerAll(startedInspection, "7.00", YesNoAnswer.YES, "VISIBLE");
        assertDoesNotThrow(() -> fixture.closeInspection.execute(startedInspection, fixture.inspector));
    }

    @Test
    void inspectionsAssignedAfterPublishingUseTheNewVersion() {
        fixture.publishSchemaVersion.execute(CertiflowFixture.EXTINGUISHER, sectionsIncludingHose());

        InspectionId inspectionAssignedLater = fixture.assignedInspection();

        assertEquals(2, fixture.inspection(inspectionAssignedLater).schemaVersion().number());
        assertDoesNotThrow(() -> fixture.recordAnswer.execute(inspectionAssignedLater, hoseCriterion, YesNoAnswer.YES, fixture.inspector));
    }

    @Test
    void publishingAVersionYieldsANewSchemaAndLeavesThePreviousOneUntouched() {
        InspectionSchema before = fixture.inspectionSchemaRepository.findByAssetType(CertiflowFixture.EXTINGUISHER).orElseThrow();

        fixture.publishSchemaVersion.execute(CertiflowFixture.EXTINGUISHER, sectionsIncludingHose());

        InspectionSchema after = fixture.inspectionSchemaRepository.findByAssetType(CertiflowFixture.EXTINGUISHER).orElseThrow();
        assertEquals(1, before.versions().size());
        assertEquals(1, before.currentVersion().number());
        assertEquals(2, after.versions().size());
        assertEquals(2, after.currentVersion().number());
        assertEquals(before.versions().getFirst(), after.versions().getFirst());
    }

    @Test
    void assetTypeAcceptsASingleSchemaAndFurtherChangesArePublishedAsVersions() {
        assertThrows(SchemaAlreadyDefinedForAssetTypeException.class, () -> fixture.createInspectionSchema.execute(
                "Another extinguisher schema", CertiflowFixture.EXTINGUISHER, fixture.firstVersionSections()));
    }

    @Test
    void versionCannotBePublishedForAnAssetTypeWithoutSchema() {
        assertThrows(NoInspectionSchemaForAssetTypeException.class,
                () -> fixture.publishSchemaVersion.execute(new AssetType("Laboratory"), fixture.firstVersionSections()));
    }

    private List<Section> sectionsIncludingHose() {
        return List.of(new Section("General condition", List.of(fixture.pressureCriterion, fixture.sealIntactCriterion, fixture.signageCriterion, hoseCriterion)));
    }
}
