package ar.edu.itba.certiflow.infrastructure.assistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.assistance.model.AssistantReply;
import ar.edu.itba.certiflow.assistance.model.CriterionUnderReview;
import ar.edu.itba.certiflow.assistance.model.EvidenceUnderReview;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ModelId;
import ar.edu.itba.certiflow.assistance.model.PromptVersion;
import ar.edu.itba.certiflow.assistance.model.ProposedFinding;
import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.assistance.model.exceptions.AssistantUnavailableException;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class SpringAiInspectionAssistantTest {

    private static final String ONE_FINDING = """
            {"findings": [{"criterion": "HOSE", "severity": "MINOR",
              "rationale": "La observación describe una grieta", "quotes": ["grieta cerca del acople"]}]}
            """;

    private final ActivePrompt prompt = new PromptCatalog().published(new PromptVersion("v1"));
    private final SeverityCatalog severities = new SeverityCatalog(List.of(StandardSeverity.values()));
    private final ModelId configuredModel = new ModelId("configured-model");

    private final InspectionUnderReview inspection = new InspectionUnderReview(new InspectionId("I-1"),
            new AssetType("Fire extinguisher"), 1, false, List.of(new CriterionUnderReview(new CriterionCode("HOSE"),
            "Hose condition", "option-in-list (options: Good)", "Good", Outcome.APPROVED,
            List.of("La manguera tiene una grieta cerca del acople"),
            List.of(new EvidenceUnderReview("PHOTO", "Foto de la manguera")))));

    private SpringAiInspectionAssistant assistantAnswering(ScriptedChatModel model) {
        return new SpringAiInspectionAssistant(ChatClient.create(model), prompt, configuredModel, severities);
    }

    @Test
    void aWellFormedAnswerBecomesProposalsTracedToThePromptAndTheModelThatAnswered() {
        AssistantReply reply = assistantAnswering(new ScriptedChatModel(ONE_FINDING, "claude-sonnet-5-5")).review(inspection);

        assertEquals(new PromptVersion("v1"), reply.promptVersion());
        assertEquals(new ModelId("claude-sonnet-5-5"), reply.modelId());
        assertEquals(List.of(new ProposedFinding(new CriterionCode("HOSE"), StandardSeverity.MINOR,
                "La observación describe una grieta", List.of(new Quote("grieta cerca del acople")))), reply.proposals());
    }

    @Test
    void theModelReceivesThePromptAndWhatTheInspectorWrote() {
        ScriptedChatModel model = new ScriptedChatModel(ONE_FINDING, "claude-sonnet-5-5");

        assistantAnswering(model).review(inspection);

        String sent = model.prompts.getFirst().getContents();
        assertTrue(sent.contains("find non-conformities that the free text describes"));
        assertTrue(sent.contains("La manguera tiene una grieta cerca del acople"));
        assertTrue(sent.contains("Foto de la manguera"));
        assertFalse(sent.contains("{inspection}"));
    }

    @Test
    void withoutTheModelNameInTheAnswerTheConfiguredOneIsRecorded() {
        AssistantReply reply = assistantAnswering(new ScriptedChatModel(ONE_FINDING, "")).review(inspection);

        assertEquals(configuredModel, reply.modelId());
    }

    @Test
    void anAnswerThatIsNotTheAgreedFormatIsUnavailable() {
        SpringAiInspectionAssistant assistant = assistantAnswering(new ScriptedChatModel("I found a crack", "m"));

        assertThrows(AssistantUnavailableException.class, () -> assistant.review(inspection));
    }

    @Test
    void anUnknownSeverityOrABlankQuoteMakesTheWholeAnswerUnavailable() {
        SpringAiInspectionAssistant unknownSeverity = assistantAnswering(new ScriptedChatModel(
                ONE_FINDING.replace("MINOR", "SEVERE"), "m"));
        SpringAiInspectionAssistant blankQuote = assistantAnswering(new ScriptedChatModel(
                ONE_FINDING.replace("grieta cerca del acople", " "), "m"));

        assertThrows(AssistantUnavailableException.class, () -> unknownSeverity.review(inspection));
        assertThrows(AssistantUnavailableException.class, () -> blankQuote.review(inspection));
    }

    @Test
    void aProviderFailureIsUnavailable() {
        SpringAiInspectionAssistant assistant = assistantAnswering(new FailingChatModel());

        assertThrows(AssistantUnavailableException.class, () -> assistant.review(inspection));
    }

    /** Responde siempre lo mismo y guarda lo que recibió. */
    private static class ScriptedChatModel implements ChatModel {

        private final String content;
        private final String model;
        private final List<Prompt> prompts = new ArrayList<>();

        ScriptedChatModel(String content, String model) {
            this.content = content;
            this.model = model;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(content))),
                    ChatResponseMetadata.builder().model(model).build());
        }
    }

    private static final class FailingChatModel extends ScriptedChatModel {

        FailingChatModel() {
            super("", "");
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new IllegalStateException("Connection refused");
        }
    }
}
