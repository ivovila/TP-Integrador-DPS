package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.SubsystemColumn;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.Criterion;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class SectionsDocumentMapper {

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

    private final StoredJson storedJson;

    public SectionsDocumentMapper(StoredJson storedJson) {
        this.storedJson = storedJson;
    }

    public String write(List<Section> sections) {
        return storedJson.write(sections.stream().map(this::sectionDocumentOf).toList());
    }

    public List<Section> read(String storedSections) {
        return storedJson.readListOf(storedSections, SectionDocument.class).stream().map(this::sectionOf).toList();
    }

    public String writeCriterion(Criterion<?> criterion) {
        return storedJson.write(criterionDocumentOf(criterion));
    }

    public Criterion<?> readCriterion(String storedCriterion) {
        return criterionOf(storedJson.read(storedCriterion, CriterionDocument.class));
    }

    private SectionDocument sectionDocumentOf(Section section) {
        return new SectionDocument(section.title(),
                section.criteria().stream().map(this::criterionDocumentOf).toList(),
                new SubsystemColumn(section.subsystem()).subsystemCode());
    }

    private Section sectionOf(SectionDocument document) {
        return new Section(document.title(),
                document.criteria().stream().<Criterion<?>>map(this::criterionOf).toList(),
                new SubsystemColumn(document.subsystemCode()).association());
    }

    private CriterionDocument criterionDocumentOf(Criterion<?> criterion) {
        return new CriterionDocument(criterion.code(), criterion.text(), ruleDocumentOf(criterion.rule()),
                criterion.severity().code(),
                criterion.requiredEvidence().stream()
                        .map(requirement -> new EvidenceDocument(requirement.kind().code(), requirement.minimum()))
                        .toList(),
                new SubsystemColumn(criterion.subsystem()).subsystemCode());
    }

    private RuleDocument ruleDocumentOf(ApprovalRule<?> rule) {
        return RULES.stream().flatMap(codec -> codec.documentOf(rule).stream()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No JSON mapping for rule " + rule.getClass().getName()));
    }

    private Criterion<?> criterionOf(CriterionDocument document) {
        List<EvidenceRequirement> evidence = document.requiredEvidence().stream()
                .map(requirement -> new EvidenceRequirement(StandardEvidenceKind.valueOf(requirement.kind()),
                        requirement.minimum()))
                .toList();
        Severity severity = StandardSeverity.valueOf(document.severity());
        ApprovalRule<?> rule = RULES.stream().filter(codec -> codec.type().equals(document.rule().type())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown rule type " + document.rule().type()))
                .read().apply(document.rule());
        return criterionOf(document, rule, severity, evidence);
    }

    private <A extends Answer> Criterion<A> criterionOf(CriterionDocument document, ApprovalRule<A> rule,
                                                        Severity severity, List<EvidenceRequirement> evidence) {
        return new Criterion<>(document.code(), document.text(), rule, severity, evidence,
                new SubsystemColumn(document.subsystemCode()).association());
    }
}
