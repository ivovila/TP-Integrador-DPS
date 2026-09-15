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
    private InspectionState state = new Assigned();
    private final Map<CriterionId, Response> originalResponses = new LinkedHashMap<>();
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
        if (!currentVersion.schemaId().equals(schemaId)) {
            throw new DomainException("La version no corresponde al esquema asignado");
        }
        state = state.start(currentVersion);
        recordEvent(new InspectionStarted(id, currentVersion.number(), now));
    }

    public void register(CriterionId criterionId, Response response) {
        state.checkCanRegister();
        checkBelongsToSchema(criterionId, response);
        originalResponses.put(criterionId, response);
    }

    public Response responseFor(CriterionId criterionId) {
        return getResponses().getOrDefault(criterionId, Response.empty());
    }

    public void close(LocalDateTime now) {
        state = state.close(evaluate(getResponses()));
        recordEvent(new InspectionClosed(id, now));
    }

    public void rectify(Rectification rectification) {
        rectification.corrections().forEach(this::checkBelongsToSchema);
        Map<CriterionId, Response> corrected = new LinkedHashMap<>(getResponses());
        corrected.putAll(rectification.corrections());
        state = state.rectify(evaluate(corrected));
        rectifications.add(rectification);
        recordEvent(new InspectionRectified(id, rectification.author(), rectification.reason(),
                rectification.rectifiedAt()));
    }

    public SchemaVersion getSchema() {
        return state.schema();
    }

    public InspectionEvaluation getEvaluation() {
        return state.evaluation();
    }

    public Map<CriterionId, Response> getResponses() {
        Map<CriterionId, Response> effective = new LinkedHashMap<>(originalResponses);
        rectifications.forEach(rectification -> effective.putAll(rectification.corrections()));
        return Collections.unmodifiableMap(effective);
    }

    public Map<CriterionId, Response> getOriginalResponses() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(originalResponses));
    }

    public List<Rectification> getRectifications() {
        return List.copyOf(rectifications);
    }

    private InspectionEvaluation evaluate(Map<CriterionId, Response> recorded) {
        List<CriterionResult> results = new ArrayList<>();
        for (Section section : getSchema().sections()) {
            for (Criterion criterion : section.criteria()) {
                Response response = recorded.getOrDefault(criterion.id(), Response.empty());
                results.add(new CriterionResult(section.name(), criterion.id(), criterion.evaluate(response),
                        response.evidences()));
            }
        }
        return new InspectionEvaluation(id, asset, results);
    }

    private void checkBelongsToSchema(CriterionId criterionId, Response response) {
        Criterion criterion = getSchema().findCriterion(criterionId)
                .orElseThrow(() -> new DomainException("El criterio no pertenece al esquema de la inspeccion"));
        response.measurement()
                .filter(measurement -> !criterion.accepts(measurement))
                .ifPresent(measurement -> {
                    throw new DomainException("El criterio no evalua la medicion '"
                            + measurement.magnitude() + "' en " + measurement.unit());
                });
    }
}
