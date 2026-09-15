package ar.edu.itba.certiflow.domain.model.inspection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.inspection.states.Assigned;
import ar.edu.itba.certiflow.domain.model.schema.Criterion;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.schema.SchemaId;
import ar.edu.itba.certiflow.domain.model.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.model.schema.Section;
import ar.edu.itba.certiflow.domain.model.shared.AggregateRoot;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.model.shared.Response;
import lombok.Getter;

@Getter
public class Inspection extends AggregateRoot {

    private final InspectionId id;
    private final AssetId asset;
    private final SchemaId schemaId;
    private final PersonId inspector;
    private final LocalDate scheduledDate;
    private final String scope;
    private SchemaVersion schema;
    private InspectionState state = new Assigned();
    private final Map<CriterionId, Response> responses = new LinkedHashMap<>();
    private final List<Rectification> rectifications = new ArrayList<>();

    private Inspection(InspectionId id, AssetId asset, SchemaId schemaId, PersonId inspector,
                       LocalDate scheduledDate, String scope) {
        this.id = id;
        this.asset = asset;
        this.schemaId = schemaId;
        this.inspector = inspector;
        this.scheduledDate = scheduledDate;
        this.scope = scope;
    }

    public static Inspection assign(InspectionId id, Asset asset, InspectionSchema schema, PersonId inspector,
                                    LocalDate scheduledDate, String scope) {
        if (!schema.getAssetType().equals(asset.getType())) {
            throw new DomainException("El esquema '" + schema.getName() + "' no aplica a activos de tipo "
                    + asset.getType().name());
        }
        return new Inspection(id, asset.getId(), schema.getId(), inspector, scheduledDate, scope);
    }

    public void start(SchemaVersion currentVersion, LocalDateTime now) {
        InspectionState next = state.start();
        if (!currentVersion.schemaId().equals(schemaId)) {
            throw new IllegalArgumentException("La version no corresponde al esquema asignado");
        }
        schema = currentVersion;
        state = next;
        recordEvent(new InspectionStarted(id, currentVersion.number(), now));
    }

    public void register(CriterionId criterionId, Response response) {
        state.checkCanRegister();
        checkApplies(criterionId, response);
        responses.put(criterionId, response);
    }

    public Response responseFor(CriterionId criterionId) {
        return effectiveResponses().getOrDefault(criterionId, Response.empty());
    }

    public void close(LocalDateTime now) {
        state = state.close();
        recordEvent(new InspectionClosed(id, now));
    }

    public void rectify(Rectification rectification) {
        InspectionState next = state.rectify();
        rectification.corrections().forEach(this::checkApplies);
        rectifications.add(rectification);
        state = next;
        recordEvent(new InspectionRectified(id, rectification.author(), rectification.reason(),
                rectification.rectifiedAt()));
    }

    public InspectionEvaluation evaluate() {
        state.checkCanEvaluate();
        Map<CriterionId, Response> effective = effectiveResponses();
        List<CriterionResult> results = new ArrayList<>();
        for (Section section : schema.sections()) {
            for (Criterion criterion : section.getCriteria()) {
                Response response = effective.getOrDefault(criterion.id(), Response.empty());
                results.add(new CriterionResult(section.getName(), criterion.id(), criterion.evaluate(response)));
            }
        }
        return new InspectionEvaluation(results);
    }

    public Map<CriterionId, Response> effectiveResponses() {
        Map<CriterionId, Response> effective = new LinkedHashMap<>(responses);
        rectifications.forEach(rectification -> effective.putAll(rectification.corrections()));
        return Collections.unmodifiableMap(effective);
    }

    public Map<CriterionId, Response> getResponses() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(responses));
    }

    public List<Rectification> getRectifications() {
        return List.copyOf(rectifications);
    }

    private void checkApplies(CriterionId criterionId, Response response) {
        Criterion criterion = schema.findCriterion(criterionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El criterio no pertenece al esquema de la inspeccion"));
        response.measurement()
                .filter(measurement -> !criterion.accepts(measurement))
                .ifPresent(measurement -> {
                    throw new IllegalArgumentException("El criterio no evalua la medicion '"
                            + measurement.magnitude() + "' en " + measurement.unit());
                });
    }
}
