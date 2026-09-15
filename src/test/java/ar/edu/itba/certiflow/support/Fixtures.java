package ar.edu.itba.certiflow.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.asset.AssetType;
import ar.edu.itba.certiflow.domain.model.asset.Characteristics;
import ar.edu.itba.certiflow.domain.model.asset.Location;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.schema.Criterion;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.schema.SchemaId;
import ar.edu.itba.certiflow.domain.model.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.model.shared.Evidence;
import ar.edu.itba.certiflow.domain.model.shared.EvidenceType;
import ar.edu.itba.certiflow.domain.model.shared.Measurement;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.model.shared.Response;
import ar.edu.itba.certiflow.domain.rules.BooleanRule;
import ar.edu.itba.certiflow.domain.rules.CompositeRule;
import ar.edu.itba.certiflow.domain.rules.NumericRangeRule;
import ar.edu.itba.certiflow.domain.rules.RequiredEvidenceRule;

public final class Fixtures {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 2, 9, 0);
    public static final AssetType EXTINGUISHER = new AssetType("matafuego");
    public static final CriterionId PRESSURE = CriterionId.generate();
    public static final CriterionId SIGNAGE = CriterionId.generate();

    private Fixtures() {
    }

    public static Asset extinguisher() {
        return new Asset(AssetId.generate(), EXTINGUISHER,
                new Location("Planta Norte", "Av. Libertador 1200", "Deposito"),
                PersonId.generate(), new Characteristics(Map.of("capacidad", "10kg")));
    }

    public static InspectionSchema extinguisherSchema() {
        InspectionSchema schema = new InspectionSchema(SchemaId.generate(), "Matafuegos", EXTINGUISHER);
        schema.addSection("Presion");
        schema.addCriterion("Presion", new Criterion(PRESSURE, "Presion de carga entre 10 y 12 bar",
                new NumericRangeRule("presion", "bar", new BigDecimal("10"), new BigDecimal("12"), BigDecimal.ONE)));
        schema.addSection("Senalizacion");
        schema.addCriterion("Senalizacion", new Criterion(SIGNAGE, "Cartel visible con foto",
                CompositeRule.allOf(new BooleanRule(true), new RequiredEvidenceRule(Set.of(EvidenceType.PHOTO)))));
        return schema;
    }

    public static Inspection closedInspection(Asset asset, InspectionSchema schema, Response pressure,
                                              Response signage) {
        Inspection inspection = Inspection.assign(InspectionId.generate(), asset, schema, PersonId.generate(),
                NOW.toLocalDate(), "Relevamiento completo");
        SchemaVersion version = schema.latestVersion().orElseGet(() -> schema.publish(NOW));
        inspection.start(version, NOW);
        inspection.register(PRESSURE, pressure);
        inspection.register(SIGNAGE, signage);
        inspection.close(NOW);
        return inspection;
    }

    public static Measurement pressure(String bar) {
        return new Measurement("presion", new BigDecimal(bar), "bar");
    }

    public static Evidence photo() {
        return new Evidence(EvidenceType.PHOTO, "cartel.jpg");
    }

    public static Response pressureResponse(String bar) {
        return Response.empty().withMeasurement(pressure(bar));
    }

    public static Response signageWithPhoto() {
        return Response.empty().withAnswer(true).withEvidence(photo());
    }
}
