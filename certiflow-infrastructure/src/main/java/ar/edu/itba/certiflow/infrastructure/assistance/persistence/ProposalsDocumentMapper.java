package ar.edu.itba.certiflow.infrastructure.assistance.persistence;

import ar.edu.itba.certiflow.assistance.model.GuardrailRejection;
import ar.edu.itba.certiflow.assistance.model.ProposedFinding;
import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.assistance.model.RejectedProposal;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.UnreadableStoredDataException;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import java.util.List;
import java.util.Optional;
import ar.edu.itba.certiflow.assistance.model.CapturedReviewInput;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ReviewInput;
import ar.edu.itba.certiflow.assistance.model.ReviewInputDescriber;
import ar.edu.itba.certiflow.assistance.model.UnavailableReviewInput;
import org.springframework.stereotype.Component;

/**
 * Lo que propuso el modelo no cambia nunca, así que se guarda como JSON dentro de la fila de la revisión, como lo
 * inmutable de un esquema (D19). Las sugerencias van en el orden de su posición.
 */
@Component
public class ProposalsDocumentMapper {

    record ProposalDocument(String criterion, String estimatedSeverity, String rationale, List<String> quotes) {
    }

    record RejectedDocument(ProposalDocument proposal, String reason) {
    }

    private final StoredJson storedJson;
    private final SeverityCatalog severities;

    public ProposalsDocumentMapper(StoredJson storedJson, SeverityCatalog severities) {
        this.storedJson = storedJson;
        this.severities = severities;
    }

    String writeProposals(List<ProposedFinding> proposals) {
        return storedJson.write(proposals.stream().map(this::documentOf).toList());
    }

    List<ProposedFinding> readProposals(String storedProposals) {
        return storedJson.readListOf(storedProposals, ProposalDocument.class).stream().map(this::proposalOf).toList();
    }

    String writeRejected(List<RejectedProposal> rejected) {
        return storedJson.write(rejected.stream()
                .map(rejection -> new RejectedDocument(documentOf(rejection.proposal()), rejection.reason().name()))
                .toList());
    }

    List<RejectedProposal> readRejected(String storedRejected) {
        return storedJson.readListOf(storedRejected, RejectedDocument.class).stream()
                .map(document -> new RejectedProposal(proposalOf(document.proposal()),
                        GuardrailRejection.valueOf(document.reason())))
                .toList();
    }

    String writeInput(ReviewInput input) {
        return input.describedBy(new ReviewInputDescriber<String>() {
            @Override
            public String captured(InspectionUnderReview inspection) {
                return storedJson.write(inspection);
            }
            @Override
            public String unavailable() {
                return null;
            }
        });
    }

    ReviewInput readInput(String input) {
        return Optional.ofNullable(input).<ReviewInput>map(value ->
                new CapturedReviewInput(storedJson.read(value, InspectionUnderReview.class)))
                .orElseGet(UnavailableReviewInput::new);
    }

    private ProposalDocument documentOf(ProposedFinding proposal) {
        return new ProposalDocument(proposal.criterion().value(), proposal.estimatedSeverity().code(),
                proposal.rationale(), proposal.quotes().stream().map(Quote::text).toList());
    }

    private ProposedFinding proposalOf(ProposalDocument document) {
        Severity severity = severities.withCode(document.estimatedSeverity())
                .orElseThrow(() -> new UnreadableStoredDataException("severity", document.estimatedSeverity()));
        return new ProposedFinding(new CriterionCode(document.criterion()), severity, document.rationale(),
                document.quotes().stream().map(Quote::new).toList());
    }
}
