package ar.edu.itba.certiflow.harness.golden;

import ar.edu.itba.certiflow.infrastructure.schema.persistence.SectionsDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import ar.edu.itba.certiflow.schema.model.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import tools.jackson.databind.json.JsonMapper;

/** Lee la misma definición versionada que el seed del frontend. No mantiene una segunda copia del esquema. */
final class DemoSchemas {
    private final SectionsDocumentMapper sections = new SectionsDocumentMapper(new StoredJson(),
            new SeverityCatalog(List.of(StandardSeverity.values())),
            new EvidenceKindCatalog(List.of(StandardEvidenceKind.values())),
            new ApprovalRuleKindCatalog(List.of(new NumericRangeRuleKind(), new YesNoRuleKind(), new OptionInListRuleKind())));

    List<Section> of(String assetType) {
        try (var input = Objects.requireNonNull(DemoSchemas.class.getResourceAsStream("/demo/schemas.json"), "Demo schemas resource")) {
            var document = JsonMapper.builder().build().readTree(input);
            return sections.read(document.required(assetType).toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read canonical demo schemas", exception);
        }
    }
}
