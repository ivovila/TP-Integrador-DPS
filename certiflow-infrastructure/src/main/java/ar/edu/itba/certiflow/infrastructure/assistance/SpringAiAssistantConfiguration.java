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

/** Configuración explícita del endpoint compatible: por defecto NVIDIA NIM, sin secretos en el repositorio. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "certiflow.assistant.provider", havingValue = "spring-ai")
public class SpringAiAssistantConfiguration {

    @Bean
    InspectionAssistant springAiInspectionAssistant(
            @Value("${certiflow.assistant.base-url:https://integrate.api.nvidia.com/v1}") String baseUrl,
            @Value("${certiflow.assistant.api-key:}") String apiKey,
            @Value("${certiflow.assistant.model:google/gemma-4-31b-it}") String model,
            @Value("${certiflow.assistant.prompt-version:v1}") String promptVersion,
            @Value("${certiflow.assistant.timeout:PT60S}") Duration timeout,
            @Value("${certiflow.assistant.enable-thinking:false}") boolean enableThinking,
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
                        .extraBody(Map.of("chat_template_kwargs", Map.of("enable_thinking", enableThinking)))
                        .temperature(0.0)
                        .maxTokens(4096)
                        .build())
                .build();
        return new SpringAiInspectionAssistant(ChatClient.create(chatModel), prompt, new ModelId(model), severities);
    }
}
