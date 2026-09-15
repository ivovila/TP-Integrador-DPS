package ar.edu.itba.certiflow.domain.model.schema;

import static ar.edu.itba.certiflow.support.Fixtures.NOW;
import static ar.edu.itba.certiflow.support.Fixtures.PRESSURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.rules.BooleanRule;
import ar.edu.itba.certiflow.support.Fixtures;

class InspectionSchemaTest {

    @Test
    void publishedVersionIsNotAffectedByLaterChanges() {
        InspectionSchema schema = Fixtures.extinguisherSchema();
        SchemaVersion first = schema.publish(NOW);

        schema.addSection("Ubicacion");
        schema.addCriterion("Ubicacion", new Criterion(CriterionId.generate(), "Accesible", new BooleanRule(true)));
        schema.addCriterion("Presion", new Criterion(CriterionId.generate(), "Sin golpes", new BooleanRule(true)));
        SchemaVersion second = schema.publish(NOW);

        assertEquals(1, first.number());
        assertEquals(2, first.criteria().size());
        assertEquals(2, second.number());
        assertEquals(4, second.criteria().size());
        assertEquals(second, schema.latestVersion().orElseThrow());
    }

    @Test
    void schemaWithoutCriteriaCannotBePublished() {
        InspectionSchema schema = new InspectionSchema(SchemaId.generate(), "Vacio", Fixtures.EXTINGUISHER);
        schema.addSection("Sin criterios");

        assertThrows(DomainException.class, () -> schema.publish(NOW));
    }

    @Test
    void sectionNamesAreUnique() {
        InspectionSchema schema = Fixtures.extinguisherSchema();

        assertThrows(IllegalArgumentException.class, () -> schema.addSection("Presion"));
    }

    @Test
    void criterionCannotBeRepeatedAcrossSections() {
        InspectionSchema schema = Fixtures.extinguisherSchema();
        Criterion duplicated = new Criterion(PRESSURE, "Otra presion", new BooleanRule(true));

        assertThrows(IllegalArgumentException.class, () -> schema.addCriterion("Senalizacion", duplicated));
    }

    @Test
    void criterionMustGoIntoAnExistingSection() {
        InspectionSchema schema = Fixtures.extinguisherSchema();
        Criterion criterion = new Criterion(CriterionId.generate(), "Accesible", new BooleanRule(true));

        assertThrows(IllegalArgumentException.class, () -> schema.addCriterion("Inexistente", criterion));
    }
}
