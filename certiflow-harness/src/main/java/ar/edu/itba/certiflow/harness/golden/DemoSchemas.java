package ar.edu.itba.certiflow.harness.golden;

import ar.edu.itba.certiflow.infrastructure.schema.persistence.SectionsDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import ar.edu.itba.certiflow.schema.model.ApprovalRuleKindCatalog;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
import ar.edu.itba.certiflow.schema.model.NumericRangeRuleKind;
import ar.edu.itba.certiflow.schema.model.OptionInListRuleKind;
import ar.edu.itba.certiflow.schema.model.Section;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.YesNoRuleKind;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Lee la misma definición versionada que el seed del frontend. No mantiene una segunda copia del esquema. */
final class DemoSchemas {

    private static final String RESOURCE = "/demo/schemas.json";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final SectionsDocumentMapper sections = new SectionsDocumentMapper(new StoredJson(),
            new SeverityCatalog(List.of(StandardSeverity.values())),
            new EvidenceKindCatalog(List.of(StandardEvidenceKind.values())),
            new ApprovalRuleKindCatalog(List.of(new NumericRangeRuleKind(), new YesNoRuleKind(), new OptionInListRuleKind())));

    List<Section> of(String assetType) {
        try (InputStream input = Optional.ofNullable(DemoSchemas.class.getResourceAsStream(RESOURCE))
                .orElseThrow(() -> new IllegalStateException("The demo schemas are missing: " + RESOURCE))) {
            JsonNode document = jsonMapper.readTree(input);
            return sections.read(document.required(assetType).toString());
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot read the demo schemas", failure);
        }
    }
}
