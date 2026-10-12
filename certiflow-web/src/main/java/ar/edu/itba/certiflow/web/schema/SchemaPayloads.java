package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.EvidenceKind;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.InvalidRuleConfigurationException;
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
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.util.Arrays;
import java.util.List;

public final class SchemaPayloads {

    private SchemaPayloads() {
    }

    public static List<Section> toSections(List<SectionPayload> sections) {
        return sections.stream()
                .map(section -> new Section(section.title(), section.criteria().stream().<Criterion<?>>map(SchemaPayloads::criterion).toList()))
                .toList();
    }

    public static List<SectionPayload> fromSections(List<Section> sections) {
        return sections.stream()
                .map(section -> new SectionPayload(section.title(), section.criteria().stream().map(SchemaPayloads::payload).toList()))
                .toList();
    }

    public static CriterionPayload payload(Criterion<?> criterion) {
        return new CriterionPayload(criterion.code(), criterion.text(), ((StandardSeverity) criterion.severity()).name(),
                rulePayload(criterion.rule()),
                criterion.requiredEvidence().stream()
                        .map(r -> new EvidenceRequirementPayload(((StandardEvidenceKind) r.kind()).name(), r.minimum())).toList());
    }

    private static Criterion<?> criterion(CriterionPayload payload) {
        Severity severity = severity(payload.severity());
        List<EvidenceRequirement> evidence = payload.requiredEvidence().stream()
                .map(r -> new EvidenceRequirement(evidenceKind(r.kind()), r.minimum())).toList();
        RulePayload rule = payload.rule();
        return switch (rule.type()) {
            case "numeric-range" -> new Criterion<>(payload.code(), payload.text(),
                    new NumericRangeRule(new Unit(rule.unit()), new Range(rule.minimum(), rule.maximum())), severity, evidence);
            case "yes-no" -> new Criterion<>(payload.code(), payload.text(), new YesNoRule(YesNoAnswer.valueOf(rule.expected())),
                    severity, evidence);
            case "option-in-list" -> new Criterion<>(payload.code(), payload.text(), new OptionInListRule(rule.options()), severity,
                    evidence);
            default -> throw new InvalidRuleConfigurationException("Unknown rule type " + rule.type());
        };
    }

    private static RulePayload rulePayload(ApprovalRule<?> rule) {
        return switch (rule) {
            case NumericRangeRule numeric -> new RulePayload("numeric-range", numeric.unit().symbol(), numeric.range().minimum(),
                    numeric.range().maximum(), null, null);
            case YesNoRule yesNo -> new RulePayload("yes-no", null, null, null, yesNo.expected().name(), null);
            case OptionInListRule option -> new RulePayload("option-in-list", null, null, null, null, option.acceptedOptions());
            default -> throw new IllegalStateException("No payload for rule " + rule.getClass().getName());
        };
    }

    private static Severity severity(String name) {
        return Arrays.stream(StandardSeverity.values()).filter(s -> s.name().equals(name)).findFirst()
                .orElseThrow(() -> new InvalidSchemaException("Unknown severity " + name));
    }

    public static EvidenceKind evidenceKind(String name) {
        return Arrays.stream(StandardEvidenceKind.values()).filter(k -> k.name().equals(name)).findFirst()
                .orElseThrow(() -> new InvalidSchemaException("Unknown evidence kind " + name));
    }
}
