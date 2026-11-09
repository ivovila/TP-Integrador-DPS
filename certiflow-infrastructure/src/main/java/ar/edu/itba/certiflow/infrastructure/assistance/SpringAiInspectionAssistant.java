package ar.edu.itba.certiflow.infrastructure.assistance;

import ar.edu.itba.certiflow.assistance.model.AssistantReply;
import ar.edu.itba.certiflow.assistance.model.CriterionUnderReview;
import ar.edu.itba.certiflow.assistance.model.EvidenceUnderReview;
import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ModelId;
import ar.edu.itba.certiflow.assistance.model.ProposedFinding;
import ar.edu.itba.certiflow.assistance.model.Quote;
import ar.edu.itba.certiflow.assistance.model.exceptions.AssistantUnavailableException;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * El asistente real, sobre Spring AI: el único lugar del sistema que conoce al framework (ArchUnit lo verifica).
 * Qué proveedor y qué modelo responden lo decide el {@code ChatModel} que recibe, que arma el starter configurado.
 * Manda la plantilla de la versión de prompt activa con la inspección en JSON, y pide la respuesta con salida
 * estructurada. Una respuesta que no se puede convertir en propuestas no es una revisión: se rechaza entera.
 */
public final class SpringAiInspectionAssistant implements InspectionAssistant {

    private static final String INSPECTION = "inspection";

    public record ModelReview(List<ModelFinding> findings) {
    }

    public record ModelFinding(String criterion, String severity, String rationale, List<String> quotes) {
    }

    record InspectionDocument(String assetType, List<CriterionDocument> criteria) {
    }

    record CriterionDocument(String code, String text, String rule, String answer, String outcome,
                             List<String> observations, List<EvidenceDocument> evidence) {
    }

    record EvidenceDocument(String kind, String description) {
    }

    private final ChatClient chatClient;
    private final ActivePrompt prompt;
    private final ModelId configuredModel;
    private final SeverityCatalog severities;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public SpringAiInspectionAssistant(ChatClient chatClient, ActivePrompt prompt, ModelId configuredModel,
                                       SeverityCatalog severities) {
        this.chatClient = chatClient;
        this.prompt = prompt;
        this.configuredModel = configuredModel;
        this.severities = severities;
    }

    /**
     * Cualquier falla, del proveedor o de una respuesta que no cumple el formato (campos que faltan, citas vacías,
     * una severidad que no existe), se traduce acá a la única excepción que el puerto promete.
     */
    @Override
    public AssistantReply review(InspectionUnderReview inspection) {
        try {
            ResponseEntity<ChatResponse, ModelReview> answer = chatClient.prompt()
                    .user(user -> user.text(prompt.template()).param(INSPECTION, documentOf(inspection)))
                    .call()
                    .responseEntity(ModelReview.class);
            return new AssistantReply(prompt.version(), modelOf(answer.response()),
                    answer.entity().findings().stream().map(this::proposalOf).toList());
        } catch (RuntimeException failure) {
            throw new AssistantUnavailableException("The assistant did not return a usable review", failure);
        }
    }

    private String documentOf(InspectionUnderReview inspection) {
        return jsonMapper.writeValueAsString(new InspectionDocument(inspection.assetType().name(),
                inspection.criteria().stream().map(this::documentOf).toList()));
    }

    private CriterionDocument documentOf(CriterionUnderReview criterion) {
        return new CriterionDocument(criterion.code().value(), criterion.text(), criterion.rule(), criterion.answer(),
                criterion.outcome().name(), criterion.observations(),
                criterion.evidence().stream().map(this::documentOf).toList());
    }

    private EvidenceDocument documentOf(EvidenceUnderReview evidence) {
        return new EvidenceDocument(evidence.kind(), evidence.description());
    }

    /** El modelo que de verdad respondió, como lo informa el proveedor; si no lo informa, el configurado. */
    private ModelId modelOf(ChatResponse response) {
        return Optional.ofNullable(response.getMetadata().getModel())
                .filter(Predicate.not(String::isBlank))
                .map(ModelId::new)
                .orElse(configuredModel);
    }

    private ProposedFinding proposalOf(ModelFinding finding) {
        Severity estimated = severities.withCode(finding.severity())
                .orElseThrow(() -> new AssistantUnavailableException("The assistant estimated an unknown severity: "
                        + finding.severity()));
        return new ProposedFinding(new CriterionCode(finding.criterion()), estimated, finding.rationale(),
                finding.quotes().stream().map(Quote::new).toList());
    }
}
