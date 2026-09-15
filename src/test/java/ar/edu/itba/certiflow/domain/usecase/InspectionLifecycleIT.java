package ar.edu.itba.certiflow.domain.usecase;

import static ar.edu.itba.certiflow.support.Fixtures.PRESSURE;
import static ar.edu.itba.certiflow.support.Fixtures.SIGNAGE;
import static ar.edu.itba.certiflow.support.Fixtures.photo;
import static ar.edu.itba.certiflow.support.Fixtures.pressure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionClosed;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionRectified;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionStarted;
import ar.edu.itba.certiflow.domain.model.schema.Criterion;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.schema.Section;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.model.shared.Response;
import ar.edu.itba.certiflow.domain.rules.BooleanRule;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;
import ar.edu.itba.certiflow.support.Fixtures;
import ar.edu.itba.certiflow.support.TestContext;

class InspectionLifecycleIT {

    private TestContext ctx;
    private Asset asset;
    private InspectionSchema schema;

    @BeforeEach
    void setUp() {
        ctx = new TestContext();
        Asset template = Fixtures.extinguisher();
        asset = ctx.registerAsset.execute(template.getType(), template.getLocation(), template.getResponsible(),
                template.getCharacteristics());
        schema = Fixtures.extinguisherSchema();
        ctx.schemas.save(schema);
        ctx.publishSchema.execute(schema.getId());
    }

    private InspectionId assignAndStart() {
        Inspection inspection = ctx.assignInspection.execute(asset.getId(), schema.getId(), PersonId.generate(),
                ctx.clock.today().plusDays(2), "Relevamiento completo");
        ctx.startInspection.execute(inspection.getId());
        return inspection.getId();
    }

    private void publishVersionWithLocationCriterion() {
        Section location = new Section("Ubicacion");
        location.addCriterion(new Criterion(CriterionId.generate(), "Accesible sin obstaculos", new BooleanRule(true)));
        schema.addSection(location);
        ctx.publishSchema.execute(schema.getId());
    }

    @Test
    void inspectionKeepsTheRulesInForceWhenItStarted() {
        InspectionId underV1 = assignAndStart();

        publishVersionWithLocationCriterion();
        InspectionId underV2 = assignAndStart();

        for (InspectionId id : List.of(underV1, underV2)) {
            ctx.registerResponse.execute(id, PRESSURE, r -> r.withMeasurement(pressure("11")));
            ctx.registerResponse.execute(id, SIGNAGE, r -> r.withAnswer(true).withEvidence(photo()));
        }
        InspectionEvaluation v1Result = ctx.closeInspection.execute(underV1);
        InspectionEvaluation v2Result = ctx.closeInspection.execute(underV2);

        assertEquals(1, ctx.inspections.findById(underV1).orElseThrow().getSchema().number());
        assertEquals(CriterionOutcome.APPROVED, v1Result.overall());
        assertEquals(2, v1Result.results().size());
        assertEquals(CriterionOutcome.REJECTED, v2Result.overall());
        assertEquals(3, v2Result.results().size());
    }

    @Test
    void versionIsTakenWhenStartingNotWhenAssigning() {
        Inspection inspection = ctx.assignInspection.execute(asset.getId(), schema.getId(), PersonId.generate(),
                ctx.clock.today().plusDays(2), "Relevamiento completo");

        publishVersionWithLocationCriterion();
        ctx.startInspection.execute(inspection.getId());

        assertEquals(2, ctx.inspections.findById(inspection.getId()).orElseThrow().getSchema().number());
    }

    @Test
    void responsesAreRegisteredProgressively() {
        InspectionId id = assignAndStart();

        ctx.registerResponse.execute(id, SIGNAGE, r -> r.withAnswer(true));
        ctx.registerResponse.execute(id, SIGNAGE, r -> r.withEvidence(photo()));
        Response signage = ctx.registerResponse.execute(id, SIGNAGE, r -> r.withObservation("Cartel algo gastado"));

        assertEquals(true, signage.answer().orElseThrow());
        assertEquals(1, signage.evidences().size());
        assertEquals(1, signage.observations().size());
    }

    @Test
    void closedInspectionIsOnlyCorrectedThroughAnAuditableRectification() {
        InspectionId id = assignAndStart();
        ctx.registerResponse.execute(id, PRESSURE, r -> r.withMeasurement(pressure("14")));
        ctx.registerResponse.execute(id, SIGNAGE, r -> r.withAnswer(true).withEvidence(photo()));
        assertEquals(CriterionOutcome.REJECTED, ctx.closeInspection.execute(id).overall());

        assertThrows(InvalidTransitionException.class,
                () -> ctx.registerResponse.execute(id, PRESSURE, r -> r.withMeasurement(pressure("11"))));

        ctx.clock.advanceDays(1);
        InspectionEvaluation corrected = ctx.rectifyInspection.execute(id, PersonId.generate(),
                "El manometro estaba descalibrado", Map.of(PRESSURE, Fixtures.pressureResponse("11")));

        Inspection inspection = ctx.inspections.findById(id).orElseThrow();
        assertEquals(CriterionOutcome.APPROVED, corrected.overall());
        assertEquals(pressure("14"), inspection.getResponses().get(PRESSURE).measurement().orElseThrow());
        assertEquals(List.of(InspectionStarted.class, InspectionClosed.class, InspectionRectified.class),
                ctx.audit.history(id.value().toString()).stream().map(Object::getClass).toList());
    }
}
