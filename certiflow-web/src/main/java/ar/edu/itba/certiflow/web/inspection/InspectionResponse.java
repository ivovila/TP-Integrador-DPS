package ar.edu.itba.certiflow.web.inspection;

import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.inspection.model.SectionsScope;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "inspections", itemRelation = "inspection")
public class InspectionResponse extends RepresentationModel<InspectionResponse> {

    public final String id;
    public final String assetCode;
    public final PersonPayload inspector;
    public final LocalDate plannedDate;
    public final int schemaVersion;
    public final Set<String> sections;
    public final boolean closed;
    public final boolean coversWholeSchema;

    InspectionResponse(InspectionView inspection) {
        this.id = inspection.id().value();
        this.assetCode = inspection.assetCode().value();
        this.inspector = PersonPayload.of(inspection.assignment().inspector());
        this.plannedDate = inspection.assignment().plannedDate();
        this.schemaVersion = inspection.schemaVersion().number();
        this.sections = inspection.assignment().scope() instanceof SectionsScope(Set<String> titles) ? new TreeSet<>(titles) : null;
        this.closed = inspection.isClosed();
        this.coversWholeSchema = inspection.coversWholeSchema();
    }
}
