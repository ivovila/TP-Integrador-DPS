package ar.edu.itba.certiflow.infrastructure.assistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.assistance.model.AssistantReply;
import ar.edu.itba.certiflow.assistance.model.CriterionUnderReview;
import ar.edu.itba.certiflow.assistance.model.EvidenceUnderReview;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ProposedFinding;
import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Outcome;
import java.util.List;
import org.junit.jupiter.api.Test;

class StubInspectionAssistantTest {

    private final StubInspectionAssistant assistant = new StubInspectionAssistant();

    private InspectionUnderReview inspectionWith(List<String> hoseObservations, List<EvidenceUnderReview> sealEvidence) {
        return new InspectionUnderReview(new InspectionId("I-1"), new AssetType("Fire extinguisher"), 1, false, List.of(
                new CriterionUnderReview(new CriterionCode("HOSE"), "Hose condition", "option-in-list", "Good",
                        Outcome.APPROVED, hoseObservations, List.of()),
                new CriterionUnderReview(new CriterionCode("SEAL"), "Seal intact", "yes-no", "true", Outcome.APPROVED,
                        List.of(), sealEvidence)));
    }

    @Test
    void aTextThatNamesAFailureIsProposedQuotedWhole() {
        AssistantReply reply = assistant.review(inspectionWith(List.of("Se limpió el soporte",
                "La manguera tiene una Grieta cerca del acople"), List.of()));

        assertEquals(1, reply.proposals().size());
        ProposedFinding proposal = reply.proposals().getFirst();
        assertEquals(new CriterionCode("HOSE"), proposal.criterion());
        assertEquals(List.of(new Quote("La manguera tiene una Grieta cerca del acople")), proposal.quotes());
        assertEquals(StubInspectionAssistant.PROMPT_VERSION, reply.promptVersion());
        assertEquals(StubInspectionAssistant.MODEL_ID, reply.modelId());
    }

    @Test
    void evidenceDescriptionsAreReadToo() {
        AssistantReply reply = assistant.review(inspectionWith(List.of(),
                List.of(new EvidenceUnderReview("PHOTO", "El precinto se ve roto en la foto"))));

        assertEquals(List.of(new CriterionCode("SEAL")), reply.proposals().stream().map(ProposedFinding::criterion).toList());
    }

    @Test
    void aCleanInspectionGetsNoProposals() {
        assertTrue(assistant.review(inspectionWith(List.of("Todo en orden"), List.of())).proposals().isEmpty());
    }

    @Test
    void theSameInspectionAlwaysGetsTheSameReply() {
        InspectionUnderReview inspection = inspectionWith(List.of("Hay corrosión en la base"), List.of());

        assertEquals(assistant.review(inspection), assistant.review(inspection));
    }
}
