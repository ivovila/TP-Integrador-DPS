package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.EvidenceKind;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidRuleConfigurationException;
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
import ar.edu.itba.certiflow.web.support.SubsystemCodeField;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class SchemaPayloads {

    /** Registro tipo de regla → cómo se arma desde el request. Agregar una regla es agregar una entrada. */
    private static final Map<String, Function<RulePayload, ApprovalRule<?>>> RULE_READERS = new LinkedHashMap<>();

    static {
        RULE_READERS.put("numeric-range", rule -> new NumericRangeRule(new Unit(required(rule.unit(), "unit")),
                new Range(required(rule.minimum(), "minimum"), required(rule.maximum(), "maximum"))));
        RULE_READERS.put("yes-no", rule -> new YesNoRule(expected(rule.expected())));
        RULE_READERS.put("option-in-list", rule -> new OptionInListRule(required(rule.options(), "options")));
    }

    /** Registro clase de regla → cómo se describe en la API. */
    private static final Map<Class<?>, Function<ApprovalRule<?>, RulePayload>> RULE_WRITERS = Map.of(
            NumericRangeRule.class, rule -> {
                NumericRangeRule numeric = (NumericRangeRule) rule;
                return new RulePayload("numeric-range", numeric.unit().symbol(), numeric.range().minimum(),
                        numeric.range().maximum(), null, null);
            },
            YesNoRule.class, rule -> new RulePayload("yes-no", null, null, null, ((YesNoRule) rule).expected().name(), null),
            OptionInListRule.class, rule -> new RulePayload("option-in-list", null, null, null, null,
                    ((OptionInListRule) rule).acceptedOptions()));

    private SchemaPayloads() {
    }

    public static List<Section> toSections(List<SectionPayload> sections) {
        return sections.stream()
                .map(section -> new Section(section.title(), section.criteria().stream().<Criterion<?>>map(SchemaPayloads::criterion).toList(), new SubsystemCodeField(section.subsystemCode()).association()))
                .toList();
    }

    public static List<SectionPayload> fromSections(List<Section> sections) {
        return sections.stream()
                .map(section -> new SectionPayload(section.title(), section.criteria().stream().map(SchemaPayloads::payload).toList(), new SubsystemCodeField(section.subsystem()).subsystemCode()))
                .toList();
    }

    public static CriterionPayload payload(Criterion<?> criterion) {
        return new CriterionPayload(criterion.code(), criterion.text(), ((StandardSeverity) criterion.severity()).name(),
                rulePayload(criterion.rule()),
                criterion.requiredEvidence().stream()
                        .map(r -> new EvidenceRequirementPayload(r.kind().code(), r.minimum())).toList(), new SubsystemCodeField(criterion.subsystem()).subsystemCode());
    }

    private static Criterion<?> criterion(CriterionPayload payload) {
        Severity severity = severity(payload.severity());
        List<EvidenceRequirement> evidence = payload.requiredEvidence().stream()
                .map(r -> new EvidenceRequirement(evidenceKind(r.kind()), r.minimum())).toList();
        return criterion(payload, rule(payload.rule()), severity, evidence);
    }

    private static <A extends Answer> Criterion<A> criterion(CriterionPayload payload, ApprovalRule<A> rule, Severity severity,
                                                            List<EvidenceRequirement> evidence) {
        return new Criterion<>(payload.code(), payload.text(), rule, severity, evidence,
                new SubsystemCodeField(payload.subsystemCode()).association());
    }

    private static ApprovalRule<?> rule(RulePayload rule) {
        return Optional.ofNullable(RULE_READERS.get(rule.type()))
                .orElseThrow(() -> new InvalidRuleConfigurationException("Unknown rule type " + rule.type()))
                .apply(rule);
    }

    private static RulePayload rulePayload(ApprovalRule<?> rule) {
        return Optional.ofNullable(RULE_WRITERS.get(rule.getClass()))
                .orElseThrow(() -> new IllegalStateException("No payload for rule " + rule.getClass().getName()))
                .apply(rule);
    }

    private static <T> T required(T value, String field) {
        return Optional.ofNullable(value)
                .orElseThrow(() -> new InvalidRuleConfigurationException("The rule needs a " + field));
    }

    private static YesNoAnswer expected(String name) {
        return Arrays.stream(YesNoAnswer.values()).filter(answer -> answer.name().equals(name)).findFirst()
                .orElseThrow(() -> new InvalidRuleConfigurationException("A yes/no rule expects YES or NO, not " + name));
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
