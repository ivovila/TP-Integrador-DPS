package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.EvidenceKind;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.NumericRangeRule;
import ar.edu.itba.certiflow.schema.model.OptionInListRule;
import ar.edu.itba.certiflow.schema.model.Range;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.Unit;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.schema.model.YesNoRule;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public final class SectionsJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    record SectionDocument(String title, List<CriterionDocument> criteria) {
    }

    record CriterionDocument(String code, String text, RuleDocument rule, String severity, List<EvidenceDocument> requiredEvidence) {
    }

    record RuleDocument(String type, String unit, BigDecimal minimum, BigDecimal maximum, String expected, Set<String> options) {
    }

    record EvidenceDocument(String kind, int minimum) {
    }

    private SectionsJson() {
    }

    public static String write(List<Section> sections) {
        return MAPPER.writeValueAsString(sections.stream()
                .map(section -> new SectionDocument(section.title(), section.criteria().stream().map(SectionsJson::document).toList()))
                .toList());
    }

    public static List<Section> read(String json) {
        List<SectionDocument> documents = MAPPER.readValue(json, new TypeReference<List<SectionDocument>>() {
        });
        return documents.stream()
                .map(document -> new Section(document.title(), document.criteria().stream().<Criterion<?>>map(SectionsJson::criterion).toList()))
                .toList();
    }

    public static String writeCriterion(Criterion<?> criterion) {
        return MAPPER.writeValueAsString(document(criterion));
    }

    public static Criterion<?> readCriterion(String json) {
        return criterion(MAPPER.readValue(json, CriterionDocument.class));
    }

    private static CriterionDocument document(Criterion<?> criterion) {
        return new CriterionDocument(criterion.code(), criterion.text(), document(criterion.rule()), severityName(criterion.severity()),
                criterion.requiredEvidence().stream()
                        .map(requirement -> new EvidenceDocument(evidenceKindName(requirement.kind()), requirement.minimum()))
                        .toList());
    }

    private static RuleDocument document(ApprovalRule<?> rule) {
        return switch (rule) {
            case NumericRangeRule numeric -> new RuleDocument("numeric-range", numeric.unit().symbol(),
                    numeric.range().minimum(), numeric.range().maximum(), null, null);
            case YesNoRule yesNo -> new RuleDocument("yes-no", null, null, null, yesNo.expected().name(), null);
            case OptionInListRule option -> new RuleDocument("option-in-list", null, null, null, null,
                    new TreeSet<>(option.acceptedOptions()));
            default -> throw new IllegalArgumentException("No JSON mapping for rule " + rule.getClass().getName());
        };
    }

    private static Criterion<?> criterion(CriterionDocument document) {
        List<EvidenceRequirement> evidence = document.requiredEvidence().stream()
                .map(requirement -> new EvidenceRequirement(evidenceKind(requirement.kind()), requirement.minimum()))
                .toList();
        Severity severity = StandardSeverity.valueOf(document.severity());
        RuleDocument rule = document.rule();
        return switch (rule.type()) {
            case "numeric-range" -> new Criterion<>(document.code(), document.text(),
                    new NumericRangeRule(new Unit(rule.unit()), new Range(rule.minimum(), rule.maximum())), severity, evidence);
            case "yes-no" -> new Criterion<>(document.code(), document.text(),
                    new YesNoRule(YesNoAnswer.valueOf(rule.expected())), severity, evidence);
            case "option-in-list" -> new Criterion<>(document.code(), document.text(),
                    new OptionInListRule(rule.options()), severity, evidence);
            default -> throw new IllegalArgumentException("Unknown rule type " + rule.type());
        };
    }

    private static String severityName(Severity severity) {
        return ((StandardSeverity) severity).name();
    }

    private static String evidenceKindName(EvidenceKind kind) {
        return ((StandardEvidenceKind) kind).name();
    }

    private static EvidenceKind evidenceKind(String name) {
        return StandardEvidenceKind.valueOf(name);
    }
}
