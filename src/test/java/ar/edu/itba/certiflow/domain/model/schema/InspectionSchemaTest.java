package ar.edu.itba.certiflow.domain.model.schema;

import static ar.edu.itba.certiflow.support.Fixtures.NOW;
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

        Section location = new Section("Ubicacion");
        location.addCriterion(new Criterion(CriterionId.generate(), "Accesible", new BooleanRule(true)));
        schema.addSection(location);
        schema.getSections().getFirst().addCriterion(
                new Criterion(CriterionId.generate(), "Sin golpes", new BooleanRule(true)));
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
        schema.addSection(new Section("Sin criterios"));

        assertThrows(DomainException.class, () -> schema.publish(NOW));
    }

    @Test
    void sectionNamesAreUnique() {
        InspectionSchema schema = Fixtures.extinguisherSchema();

        assertThrows(IllegalArgumentException.class, () -> schema.addSection(new Section("Presion")));
    }
}
