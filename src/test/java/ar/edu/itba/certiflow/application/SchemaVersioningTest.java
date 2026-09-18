package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.application.schema.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.application.schema.SchemaAlreadyDefinedForAssetTypeException;
import ar.edu.itba.certiflow.domain.asset.AssetType;
import ar.edu.itba.certiflow.details.evaluation.StandardSeverity;
import ar.edu.itba.certiflow.details.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.details.evaluation.YesNoRule;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.schema.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.domain.schema.Section;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaVersioningTest {

    private final CertiflowFixture app = new CertiflowFixture();
    private final Criterion<YesNoAnswer> hose = new Criterion<>("EXT-04", "Hose shows no cracks",
            new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of());

    @Test
    void newSchemaVersionDoesNotAffectAnInspectionAlreadyStarted() {
        Inspection started = app.assignedInspection();

        app.publishSchemaVersion.execute(CertiflowFixture.EXTINGUISHER, sectionsIncludingHose());

        assertEquals(1, started.schemaVersion().number());
        assertThrows(CriterionNotInSchemaException.class,
                () -> app.recordAnswer.execute(started, hose, YesNoAnswer.YES, app.inspector));
        app.answerAll(started, "7.00", YesNoAnswer.YES, "VISIBLE");
        assertDoesNotThrow(() -> app.closeInspection.execute(started, app.inspector));
    }

    @Test
    void inspectionsAssignedAfterPublishingUseTheNewVersion() {
        app.publishSchemaVersion.execute(CertiflowFixture.EXTINGUISHER, sectionsIncludingHose());

        Inspection assignedLater = app.assignedInspection();

        assertEquals(2, assignedLater.schemaVersion().number());
        assertDoesNotThrow(() -> app.recordAnswer.execute(assignedLater, hose, YesNoAnswer.YES, app.inspector));
    }

    @Test
    void assetTypeAcceptsASingleSchemaAndFurtherChangesArePublishedAsVersions() {
        assertThrows(SchemaAlreadyDefinedForAssetTypeException.class, () -> app.createInspectionSchema.execute(
                "Another extinguisher schema", CertiflowFixture.EXTINGUISHER, app.firstVersionSections()));
    }

    @Test
    void versionCannotBePublishedForAnAssetTypeWithoutSchema() {
        assertThrows(NoInspectionSchemaForAssetTypeException.class,
                () -> app.publishSchemaVersion.execute(new AssetType("Laboratory"), app.firstVersionSections()));
    }

    private List<Section> sectionsIncludingHose() {
        return List.of(new Section("General condition", List.of(app.pressure, app.sealIntact, app.signage, hose)));
    }
}
