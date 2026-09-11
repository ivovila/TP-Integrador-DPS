package ar.edu.itba.certiflow.domain.inspection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.asset.AssetId;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.schema.CriterionId;
import ar.edu.itba.certiflow.domain.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.shared.Measurement;
import ar.edu.itba.certiflow.domain.shared.PersonId;
import lombok.Getter;

@Getter
public class Inspection {

    private final InspectionId id;
    private final AssetId asset;
    private final SchemaVersion schema;
    private final PersonId inspector;
    private final LocalDate scheduledDate;
    private final String scope;
    private InspectionState state = new Assigned();
    private final List<InspectionRecord> records = new ArrayList<>();

    public Inspection(InspectionId id, AssetId asset, SchemaVersion schema, PersonId inspector,
                      LocalDate scheduledDate, String scope) {
        this.id = id;
        this.asset = asset;
        this.schema = schema;
        this.inspector = inspector;
        this.scheduledDate = scheduledDate;
        this.scope = scope;
    }

    public void start() {
        state = state.start();
    }

    public void register(InspectionRecord record) {
        state.checkCanRegister();
        Criterion criterion = findCriterion(record.criterion())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El criterio no pertenece al esquema de la inspeccion"));
        Measurement measurement = record.measurement();
        if (measurement != null && !criterion.expects(measurement)) {
            throw new IllegalArgumentException("El criterio no evalua la medicion '"
                    + measurement.magnitude() + "' en " + measurement.unit());
        }
        records.add(record);
    }

    public void close() {
        state = state.close();
    }

    public List<InspectionRecord> getRecords() {
        return List.copyOf(records);
    }

    private Optional<Criterion> findCriterion(CriterionId criterionId) {
        return schema.sections().stream()
                .flatMap(section -> section.getCriteria().stream())
                .filter(c -> c.id().equals(criterionId))
                .findFirst();
    }
}
