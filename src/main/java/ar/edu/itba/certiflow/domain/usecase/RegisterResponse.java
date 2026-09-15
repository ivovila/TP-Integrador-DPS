package ar.edu.itba.certiflow.domain.usecase;

import java.util.function.UnaryOperator;

import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.model.shared.Response;
import ar.edu.itba.certiflow.domain.ports.InspectionRepository;

public class RegisterResponse {

    private final InspectionRepository inspections;

    public RegisterResponse(InspectionRepository inspections) {
        this.inspections = inspections;
    }

    public Response execute(InspectionId inspectionId, CriterionId criterionId, UnaryOperator<Response> change) {
        Inspection inspection = inspections.findById(inspectionId)
                .orElseThrow(() -> new NotFoundException("la inspeccion", inspectionId.value()));
        Response updated = change.apply(inspection.responseFor(criterionId));
        inspection.register(criterionId, updated);
        inspections.save(inspection);
        return updated;
    }
}
