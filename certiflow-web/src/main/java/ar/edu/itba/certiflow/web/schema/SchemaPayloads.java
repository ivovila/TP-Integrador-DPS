package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.ApprovalRuleKindCatalog;
import ar.edu.itba.certiflow.schema.model.RuleDescriber;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.EvidenceKind;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.SectionTitle;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidRuleConfigurationException;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import ar.edu.itba.certiflow.web.support.SubsystemCodeField;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SchemaPayloads {

    private SchemaPayloads() {
    }

    public static List<Section> toSections(List<SectionPayload> sections, ApprovalRuleKindCatalog ruleKinds) {
        return sections.stream()
                .map(section -> new Section(new SectionTitle(section.title()), section.criteria().stream().<Criterion<?>>map(payload -> criterion(payload, ruleKinds)).toList(), new SubsystemCodeField(section.subsystemCode()).association()))
                .toList();
    }

    public static List<SectionPayload> fromSections(List<Section> sections) {
        return sections.stream()
                .map(section -> new SectionPayload(section.title().value(), section.criteria().stream().map(SchemaPayloads::payload).toList(), new SubsystemCodeField(section.subsystem()).subsystemCode()))
                .toList();
    }

    public static CriterionPayload payload(Criterion<?> criterion) {
        return new CriterionPayload(criterion.code().value(), criterion.text(), criterion.severity().code(),
                rulePayload(criterion.rule()),
                criterion.requiredEvidence().stream()
                        .map(r -> new EvidenceRequirementPayload(r.kind().code(), r.minimum())).toList(), new SubsystemCodeField(criterion.subsystem()).subsystemCode());
    }

    private static Criterion<?> criterion(CriterionPayload payload, ApprovalRuleKindCatalog ruleKinds) {
        Severity severity = severity(payload.severity());
        List<EvidenceRequirement> evidence = payload.requiredEvidence().stream()
                .map(r -> new EvidenceRequirement(evidenceKind(r.kind()), r.minimum())).toList();
        ApprovalRule<?> rule = ruleKinds.named(payload.rule().type())
                .orElseThrow(() -> new InvalidRuleConfigurationException("Unknown rule type " + payload.rule().type()))
                .ruleFrom(payload.rule());
        return criterion(payload, rule, severity, evidence);
    }

    private static <A extends Answer> Criterion<A> criterion(CriterionPayload payload, ApprovalRule<A> rule, Severity severity,
                                                            List<EvidenceRequirement> evidence) {
        return new Criterion<>(new CriterionCode(payload.code()), payload.text(), rule, severity, evidence,
                new SubsystemCodeField(payload.subsystemCode()).association());
    }

    private static RulePayload rulePayload(ApprovalRule<?> rule) {
        return rule.describedBy(new PayloadDescriber());
    }

    private static final class PayloadDescriber implements RuleDescriber<RulePayload> {
        private final Map<String, Object> parameters = new LinkedHashMap<>();

        @Override
        public RuleDescriber<RulePayload> text(String name, String value) { parameters.put(name, value); return this; }
        @Override
        public RuleDescriber<RulePayload> decimal(String name, java.math.BigDecimal value) { parameters.put(name, value); return this; }
        @Override
        public RuleDescriber<RulePayload> texts(String name, Set<String> value) { parameters.put(name, value); return this; }
        @Override
        public RulePayload ofKind(String kind) { return RulePayload.of(kind, parameters); }
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
