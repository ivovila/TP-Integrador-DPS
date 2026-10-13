package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.certification.usecase.ListJurisdictionsUseCase;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.util.Arrays;
import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularyController {

    public record SeverityView(String name, String label, String outcomeWhenUnmet) {
    }

    public record EvidenceKindView(String name, String label) {
    }

    public record JurisdictionView(String code, String policyCode) {
    }

    public static class VocabularyResource extends RepresentationModel<VocabularyResource> {

        public final List<SeverityView> severities;
        public final List<EvidenceKindView> evidenceKinds;
        public final List<JurisdictionView> jurisdictions;

        VocabularyResource(List<SeverityView> severities, List<EvidenceKindView> evidenceKinds, List<JurisdictionView> jurisdictions) {
            this.severities = severities;
            this.evidenceKinds = evidenceKinds;
            this.jurisdictions = jurisdictions;
        }
    }

    private final ListJurisdictionsUseCase listJurisdictions;

    public VocabularyController(ListJurisdictionsUseCase listJurisdictions) {
        this.listJurisdictions = listJurisdictions;
    }

    @GetMapping
    public ResponseEntity<VocabularyResource> vocabulary() {
        VocabularyResource vocabulary = new VocabularyResource(
                Arrays.stream(StandardSeverity.values())
                        .map(severity -> new SeverityView(severity.name(), severity.label(), severity.outcomeWhenUnmet().name()))
                        .toList(),
                Arrays.stream(StandardEvidenceKind.values())
                        .map(evidenceKind -> new EvidenceKindView(evidenceKind.name(), evidenceKind.label()))
                        .toList(),
                listJurisdictions.execute().stream()
                        .map(registered -> new JurisdictionView(registered.jurisdictionCode().value(), registered.policyCode()))
                        .toList());
        vocabulary.add(Link.of("/api/v1/vocabulary").withSelfRel());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(vocabulary);
    }
}
