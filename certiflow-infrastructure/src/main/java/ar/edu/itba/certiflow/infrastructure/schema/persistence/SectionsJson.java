package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
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
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import ar.edu.itba.certiflow.schema.model.Answer;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class SectionsJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    record SectionDocument(String title, List<CriterionDocument> criteria, String subsystemCode) {
    }

    record CriterionDocument(String code, String text, RuleDocument rule, String severity, List<EvidenceDocument> requiredEvidence, String subsystemCode) {
    }

    record RuleDocument(String type, String unit, BigDecimal minimum, BigDecimal maximum, String expected, Set<String> options) {
    }

    record EvidenceDocument(String kind, int minimum) {
    }

    /** Cómo se guarda cada tipo de regla. Un tipo de regla nuevo es una entrada más. */
    private record RuleCodec<R extends ApprovalRule<?>>(String type, Class<R> ruleClass,
                                                       BiFunction<String, R, RuleDocument> write,
                                                       Function<RuleDocument, ApprovalRule<?>> read) {
        Optional<RuleDocument> documentOf(ApprovalRule<?> rule) {
            return Optional.of(rule).filter(ruleClass::isInstance).map(ruleClass::cast).map(typed -> write.apply(type, typed));
        }
    }

    private static final List<RuleCodec<?>> RULES = List.of(
            new RuleCodec<>("numeric-range", NumericRangeRule.class,
                    (type, numeric) -> new RuleDocument(type, numeric.unit().symbol(), numeric.range().minimum(),
                            numeric.range().maximum(), null, null),
                    document -> new NumericRangeRule(new Unit(document.unit()), new Range(document.minimum(), document.maximum()))),
            new RuleCodec<>("yes-no", YesNoRule.class,
                    (type, yesNo) -> new RuleDocument(type, null, null, null, yesNo.expected().name(), null),
                    document -> new YesNoRule(YesNoAnswer.valueOf(document.expected()))),
            new RuleCodec<>("option-in-list", OptionInListRule.class,
                    (type, option) -> new RuleDocument(type, null, null, null, null, new TreeSet<>(option.acceptedOptions())),
                    document -> new OptionInListRule(document.options())));

    private SectionsJson() {
    }

    public static String write(List<Section> sections) {
        return MAPPER.writeValueAsString(sections.stream()
                .map(section -> new SectionDocument(section.title(), section.criteria().stream().map(SectionsJson::document).toList(), section.subsystemCode().map(code -> code.value()).orElse(null)))
                .toList());
    }

    public static List<Section> read(String json) {
        List<SectionDocument> documents = MAPPER.readValue(json, new TypeReference<List<SectionDocument>>() {
        });
        return documents.stream()
                .map(document -> new Section(document.title(), document.criteria().stream().<Criterion<?>>map(SectionsJson::criterion).toList(), Optional.ofNullable(document.subsystemCode()).map(SubsystemCode::new)))
                .toList();
    }

    public static String writeCriterion(Criterion<?> criterion) {
        return MAPPER.writeValueAsString(document(criterion));
    }

    public static Criterion<?> readCriterion(String json) {
        return criterion(MAPPER.readValue(json, CriterionDocument.class));
    }

    private static CriterionDocument document(Criterion<?> criterion) {
        return new CriterionDocument(criterion.code(), criterion.text(), document(criterion.rule()), criterion.severity().code(),
                criterion.requiredEvidence().stream()
                        .map(requirement -> new EvidenceDocument(requirement.kind().code(), requirement.minimum()))
                        .toList(), criterion.subsystemCode().map(code -> code.value()).orElse(null));
    }

    private static RuleDocument document(ApprovalRule<?> rule) {
        return RULES.stream().flatMap(codec -> codec.documentOf(rule).stream()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No JSON mapping for rule " + rule.getClass().getName()));
    }

    private static Criterion<?> criterion(CriterionDocument document) {
        List<EvidenceRequirement> evidence = document.requiredEvidence().stream()
                .map(requirement -> new EvidenceRequirement(evidenceKind(requirement.kind()), requirement.minimum()))
                .toList();
        Severity severity = StandardSeverity.valueOf(document.severity());
        ApprovalRule<?> rule = RULES.stream().filter(codec -> codec.type().equals(document.rule().type())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown rule type " + document.rule().type()))
                .read().apply(document.rule());
        return criterion(document, rule, severity, evidence);
    }

    private static <A extends Answer> Criterion<A> criterion(CriterionDocument document, ApprovalRule<A> rule, Severity severity,
                                                             List<EvidenceRequirement> evidence) {
        return new Criterion<>(document.code(), document.text(), rule, severity, evidence,
                Optional.ofNullable(document.subsystemCode()).map(SubsystemCode::new));
    }

    private static EvidenceKind evidenceKind(String name) {
        return StandardEvidenceKind.valueOf(name);
    }
}
