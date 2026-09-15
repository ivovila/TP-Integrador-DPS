package ar.edu.itba.certiflow.domain.model.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.inspection.Rectification;
import ar.edu.itba.certiflow.domain.model.schema.Criterion;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.model.schema.Section;
import ar.edu.itba.certiflow.domain.model.shared.Evidence;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.model.shared.Response;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;

public record InspectionReport(InspectionId inspection, AssetId asset, String schemaName, int schemaVersion,
                               PersonId inspector, LocalDate scheduledDate, String scope, CriterionOutcome overall,
                               List<SectionLine> sections, List<Rectification> rectifications) {

    public InspectionReport {
        sections = List.copyOf(sections);
        rectifications = List.copyOf(rectifications);
    }

    public static InspectionReport of(Inspection inspection) {
        InspectionEvaluation evaluation = inspection.getEvaluation();
        SchemaVersion schema = inspection.getSchema();
        Map<CriterionId, Response> responses = inspection.getResponses();
        List<SectionLine> sections = schema.sections().stream()
                .map(section -> sectionLine(section, evaluation, responses))
                .toList();
        return new InspectionReport(inspection.getId(), inspection.getAsset(), schema.name(), schema.number(),
                inspection.getInspector(), inspection.getScheduledDate(), inspection.getScope(), evaluation.overall(),
                sections, inspection.getRectifications());
    }

    private static SectionLine sectionLine(Section section, InspectionEvaluation evaluation,
                                           Map<CriterionId, Response> responses) {
        List<CriterionLine> criteria = section.criteria().stream()
                .map(criterion -> criterionLine(criterion, evaluation,
                        responses.getOrDefault(criterion.id(), Response.empty())))
                .toList();
        return new SectionLine(section.name(), evaluation.sectionOutcome(section.name()), criteria);
    }

    private static CriterionLine criterionLine(Criterion criterion, InspectionEvaluation evaluation,
                                               Response response) {
        return new CriterionLine(criterion.description(), evaluation.outcomeOf(criterion.id()),
                response.evidences(), response.observations());
    }

    public record SectionLine(String name, CriterionOutcome outcome, List<CriterionLine> criteria) {

        public SectionLine {
            criteria = List.copyOf(criteria);
        }
    }

    public record CriterionLine(String description, CriterionOutcome outcome, List<Evidence> evidences,
                                List<String> observations) {

        public CriterionLine {
            evidences = List.copyOf(evidences);
            observations = List.copyOf(observations);
        }
    }
}
