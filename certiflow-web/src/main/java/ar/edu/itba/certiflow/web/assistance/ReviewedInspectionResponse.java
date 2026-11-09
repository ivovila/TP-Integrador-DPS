package ar.edu.itba.certiflow.web.assistance;

import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ReviewInputDescriber;
import java.util.List;

/** DTO propio: lo enviado al modelo, sin autores ni referencias de archivos. */
public record ReviewedInspectionResponse(String assetType, int schemaVersion, List<Criterion> criteria) {
    public record Evidence(String kind, String description) { }
    public record Criterion(String code, String text, String rule, String answer, String outcome,
                            List<String> observations, List<Evidence> evidence) { }

    static final class Describer implements ReviewInputDescriber<ReviewedInspectionResponse> {
        @Override
        public ReviewedInspectionResponse captured(InspectionUnderReview inspection) {
            return new ReviewedInspectionResponse(inspection.assetType().name(), inspection.schemaVersion(),
                    inspection.criteria().stream().map(criterion -> new Criterion(criterion.code().value(), criterion.text(),
                            criterion.rule(), criterion.answer(), criterion.outcome().name(), criterion.observations(),
                            criterion.evidence().stream().map(evidence -> new Evidence(evidence.kind(), evidence.description())).toList()))
                            .toList());
        }
        @Override
        public ReviewedInspectionResponse unavailable() {
            return null;
        }
    }
}
