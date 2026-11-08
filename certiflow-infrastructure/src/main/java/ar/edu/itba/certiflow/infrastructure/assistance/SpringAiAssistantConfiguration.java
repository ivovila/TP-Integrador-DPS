package ar.edu.itba.certiflow.infrastructure.assistance;

import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.assistance.model.ModelId;
import ar.edu.itba.certiflow.assistance.model.PromptVersion;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import java.time.Duration;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuración explícita del endpoint compatible con OpenAI: por defecto NVIDIA NIM con Nemotron 3.5 Super, sin
 * secretos en el repositorio. Lo que cada modelo acepta de más (cómo se apaga su razonamiento, cuánto espera la
 * cola del proveedor) va como JSON en {@code extra-body} y {@code headers}, así cambiar de modelo es configuración
 * y no código.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "certiflow.assistant.provider", havingValue = "spring-ai")
public class SpringAiAssistantConfiguration {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Bean
    InspectionAssistant springAiInspectionAssistant(
            @Value("${certiflow.assistant.base-url:https://integrate.api.nvidia.com/v1}") String baseUrl,
            @Value("${certiflow.assistant.api-key:}") String apiKey,
            @Value("${certiflow.assistant.model:nvidia/nemotron-3.5-super-vl-120b-a12b}") String model,
            @Value("${certiflow.assistant.prompt-version:v1}") String promptVersion,
            @Value("${certiflow.assistant.timeout:PT300S}") Duration timeout,
            @Value("${certiflow.assistant.extra-body:{}}") String extraBody,
            @Value("${certiflow.assistant.headers:{}}") String headers,
            SeverityCatalog severities) {
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("Configure certiflow.assistant.api-key before selecting spring-ai");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("certiflow.assistant.timeout must be positive");
        }
        ActivePrompt prompt = new PromptCatalog().published(new PromptVersion(promptVersion));
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .model(model)
                        .timeout(timeout)
                        .maxRetries(0)
                        .extraBody(jsonObject(extraBody, Object.class))
                        .customHeaders(jsonObject(headers, String.class))
                        .temperature(0.0)
                        .maxTokens(4096)
                        .build())
                .build();
        return new SpringAiInspectionAssistant(ChatClient.create(chatModel), prompt, new ModelId(model), severities);
    }

    private <V> Map<String, V> jsonObject(String json, Class<V> valueType) {
        return jsonMapper.readValue(json, jsonMapper.getTypeFactory().constructMapType(Map.class, String.class, valueType));
    }
}
