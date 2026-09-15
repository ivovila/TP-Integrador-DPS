package ar.edu.itba.certiflow.domain.model.inspection;

import static ar.edu.itba.certiflow.support.Fixtures.NOW;
import static ar.edu.itba.certiflow.support.Fixtures.PRESSURE;
import static ar.edu.itba.certiflow.support.Fixtures.SIGNAGE;
import static ar.edu.itba.certiflow.support.Fixtures.pressureResponse;
import static ar.edu.itba.certiflow.support.Fixtures.signageWithPhoto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetType;
import ar.edu.itba.certiflow.domain.model.inspection.states.Rectified;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.schema.SchemaId;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;
import ar.edu.itba.certiflow.domain.model.shared.Measurement;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.model.shared.Response;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;
import ar.edu.itba.certiflow.support.Fixtures;

class InspectionTest {

    private final Asset asset = Fixtures.extinguisher();
    private final InspectionSchema schema = Fixtures.extinguisherSchema();

    private Inspection assigned() {
        return Inspection.assign(InspectionId.generate(), asset, schema, PersonId.generate(),
                NOW.toLocalDate(), "Relevamiento completo");
    }

    private Inspection started() {
        Inspection inspection = assigned();
        inspection.start(schema.publish(NOW), NOW);
        return inspection;
    }

    @Test
    void schemaMustApplyToAssetType() {
        InspectionSchema otherType = new InspectionSchema(SchemaId.generate(), "Tableros", new AssetType("tablero"));

        assertThrows(DomainException.class, () -> Inspection.assign(InspectionId.generate(), asset, otherType,
                PersonId.generate(), NOW.toLocalDate(), "Relevamiento completo"));
    }

    @Test
    void responsesCannotBeRegisteredBeforeStarting() {
        Inspection inspection = assigned();

        assertThrows(InvalidTransitionException.class, () -> inspection.register(PRESSURE, pressureResponse("11")));
    }

    @Test
    void closedInspectionCannotBeModified() {
        Inspection inspection = started();
        inspection.close(NOW);

        assertThrows(InvalidTransitionException.class, () -> inspection.register(PRESSURE, pressureResponse("11")));
    }

    @Test
    void criterionMustBelongToTheSchema() {
        Inspection inspection = started();

        assertThrows(DomainException.class,
                () -> inspection.register(CriterionId.generate(), pressureResponse("11")));
    }

    @Test
    void measurementMustBeOneTheCriterionEvaluates() {
        Inspection inspection = started();
        Response temperature = Response.empty().withMeasurement(new Measurement("temperatura", BigDecimal.TEN, "C"));

        assertThrows(DomainException.class, () -> inspection.register(PRESSURE, temperature));
    }

    @Test
    void evaluationRequiresAClosedInspection() {
        Inspection inspection = started();

        assertThrows(InvalidTransitionException.class, inspection::getEvaluation);
    }

    @Test
    void evaluatesEveryCriterionOfTheVersionInForce() {
        Inspection inspection = started();
        inspection.register(PRESSURE, pressureResponse("12.5"));
        inspection.close(NOW);

        InspectionEvaluation evaluation = inspection.getEvaluation();

        assertEquals(CriterionOutcome.OBSERVED, evaluation.outcomeOf(PRESSURE));
        assertEquals(CriterionOutcome.REJECTED, evaluation.outcomeOf(SIGNAGE));
        assertEquals(CriterionOutcome.OBSERVED, evaluation.sectionOutcome("Presion"));
        assertEquals(CriterionOutcome.REJECTED, evaluation.overall());
    }

    @Test
    void rectificationCorrectsTheResultWithoutAlteringTheOriginal() {
        Inspection inspection = started();
        inspection.register(PRESSURE, pressureResponse("14"));
        inspection.register(SIGNAGE, signageWithPhoto());
        inspection.close(NOW);
        PersonId supervisor = PersonId.generate();

        inspection.rectify(new Rectification(supervisor, "Manometro mal calibrado", NOW.plusDays(1),
                Map.of(PRESSURE, pressureResponse("11"))));

        assertInstanceOf(Rectified.class, inspection.getState());
        assertEquals(pressureResponse("14").measurement(), inspection.getResponses().get(PRESSURE).measurement());
        assertEquals(CriterionOutcome.APPROVED, inspection.getEvaluation().overall());
        assertEquals(1, inspection.getRectifications().size());
    }

    @Test
    void closingFixesTheEvaluationWithTheEvidenceUsed() {
        Inspection inspection = started();
        inspection.register(SIGNAGE, signageWithPhoto());
        inspection.close(NOW);

        CriterionResult signage = inspection.getEvaluation().resultOf(SIGNAGE);

        assertEquals(CriterionOutcome.APPROVED, signage.outcome());
        assertEquals(1, signage.evidences().size());
        assertEquals(inspection.getId(), inspection.getEvaluation().inspection());
    }

    @Test
    void schemaIsOnlyKnownOnceStarted() {
        Inspection inspection = assigned();

        assertThrows(InvalidTransitionException.class, inspection::getSchema);
    }

    @Test
    void onlyClosedInspectionsCanBeRectified() {
        Inspection inspection = started();
        Rectification rectification = new Rectification(PersonId.generate(), "Correccion", NOW,
                Map.of(PRESSURE, pressureResponse("11")));

        assertThrows(InvalidTransitionException.class, () -> inspection.rectify(rectification));
    }
}
