package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.SubsystemColumn;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.UnreadableStoredDataException;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.ApprovalRule;
import ar.edu.itba.certiflow.schema.model.ApprovalRuleKindCatalog;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.EvidenceKind;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
import ar.edu.itba.certiflow.schema.model.EvidenceRequirement;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.SectionTitle;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** Las secciones de una versión, y el criterio de un hallazgo, guardados como documentos JSON. */
@Component
public class SectionsDocumentMapper {

    record SectionDocument(String title, List<CriterionDocument> criteria, String subsystemCode) {
    }

    record CriterionDocument(String code, String text, Map<String, JsonNode> rule, String severity,
                             List<EvidenceDocument> requiredEvidence, String subsystemCode) {
    }

    record EvidenceDocument(String kind, int minimum) {
    }

    private final StoredJson storedJson;
    private final SeverityCatalog severities;
    private final EvidenceKindCatalog evidenceKinds;
    private final ApprovalRuleKindCatalog ruleKinds;

    public SectionsDocumentMapper(StoredJson storedJson, SeverityCatalog severities, EvidenceKindCatalog evidenceKinds,
                                  ApprovalRuleKindCatalog ruleKinds) {
        this.storedJson = storedJson;
        this.severities = severities;
        this.evidenceKinds = evidenceKinds;
        this.ruleKinds = ruleKinds;
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
        return new SectionDocument(section.title().value(),
                section.criteria().stream().map(this::criterionDocumentOf).toList(),
                new SubsystemColumn(section.subsystem()).subsystemCode());
    }

    private Section sectionOf(SectionDocument document) {
        return new Section(new SectionTitle(document.title()),
                document.criteria().stream().<Criterion<?>>map(this::criterionOf).toList(),
                new SubsystemColumn(document.subsystemCode()).association());
    }

    private CriterionDocument criterionDocumentOf(Criterion<?> criterion) {
        return new CriterionDocument(criterion.code().value(), criterion.text(),
                criterion.rule().describedBy(new StoredRuleDescriber()), criterion.severity().code(),
                criterion.requiredEvidence().stream()
                        .map(requirement -> new EvidenceDocument(requirement.kind().code(), requirement.minimum()))
                        .toList(),
                new SubsystemColumn(criterion.subsystem()).subsystemCode());
    }

    private Criterion<?> criterionOf(CriterionDocument document) {
        StoredRuleParameters storedRule = new StoredRuleParameters(document.rule());
        ApprovalRule<?> rule = ruleKinds.named(storedRule.ruleKindName())
                .orElseThrow(() -> new UnreadableStoredDataException("rule type", storedRule.ruleKindName()))
                .ruleFrom(storedRule);
        return criterionOf(document, rule);
    }

    private <A extends Answer> Criterion<A> criterionOf(CriterionDocument document, ApprovalRule<A> rule) {
        Severity severity = severities.withCode(document.severity())
                .orElseThrow(() -> new UnreadableStoredDataException("severity", document.severity()));
        List<EvidenceRequirement> requiredEvidence = document.requiredEvidence().stream()
                .map(requirement -> new EvidenceRequirement(evidenceKindOf(requirement), requirement.minimum()))
                .toList();
        return new Criterion<>(new CriterionCode(document.code()), document.text(), rule, severity, requiredEvidence,
                new SubsystemColumn(document.subsystemCode()).association());
    }

    private EvidenceKind evidenceKindOf(EvidenceDocument requirement) {
        return evidenceKinds.withCode(requirement.kind())
                .orElseThrow(() -> new UnreadableStoredDataException("evidence kind", requirement.kind()));
    }
}
