package ar.edu.itba.certiflow.infrastructure.assistance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.assistance.model.AssistantReply;
import ar.edu.itba.certiflow.assistance.model.CriterionUnderReview;
import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.assistance.model.InspectionUnderReview;
import ar.edu.itba.certiflow.assistance.model.ModelId;
import ar.edu.itba.certiflow.assistance.model.PromptVersion;
import ar.edu.itba.certiflow.assistance.model.exceptions.AssistantUnavailableException;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.schema.model.SeverityCatalog;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.convert.ApplicationConversionService;
import tools.jackson.databind.json.JsonMapper;

/** Contrato HTTP del cliente real contra un servidor local, sin API key real ni llamadas externas. */
class NvidiaAssistantConfigurationTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final SeverityCatalog severities = new SeverityCatalog(List.of(StandardSeverity.values()));
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(SpringAiAssistantConfiguration.class)
            .withBean(SeverityCatalog.class, () -> severities);
    private final InspectionUnderReview inspection = new InspectionUnderReview(new InspectionId("I-1"),
            new AssetType("Fire extinguisher"), 1, false, List.of(new CriterionUnderReview(new CriterionCode("HOSE"),
            "Hose condition", "option-in-list (options: Good)", "Good", Outcome.APPROVED,
            List.of("La manguera tiene una grieta"), List.of())));
    private HttpServer server;

    @BeforeEach
    void startEndpoint() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stopEndpoint() {
        server.stop(0);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    @Test
    void theRealClientSendsTheConfiguredModelAndTokenAndParsesACompletion() {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> request = new AtomicReference<>();
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String content = mapper.writeValueAsString(Map.of("findings", List.of(Map.of(
                    "criterion", "HOSE", "severity", "MINOR", "rationale", "Una grieta", "quotes", List.of("una grieta")))));
            byte[] response = mapper.writeValueAsBytes(Map.of("id", "completion-1", "object", "chat.completion", "created", 0,
                    "model", "served-model", "choices", List.of(Map.of("index", 0, "finish_reason", "stop",
                            "message", Map.of("role", "assistant", "content", content)))));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        contextRunner.withPropertyValues("certiflow.assistant.provider=spring-ai", "certiflow.assistant.api-key=local-test-key",
                "certiflow.assistant.base-url=" + baseUrl(), "certiflow.assistant.model=test-model")
                .run(context -> {
                    assertThat(context).hasSingleBean(InspectionAssistant.class);
                    AssistantReply reply = context.getBean(InspectionAssistant.class).review(inspection);
                    assertEquals("Bearer local-test-key", authorization.get());
                    assertEquals("test-model", mapper.readTree(request.get()).get("model").asText());
                    assertEquals(4096, mapper.readTree(request.get()).get("max_tokens").asInt());
                    assertFalse(mapper.readTree(request.get()).at("/chat_template_kwargs/enable_thinking").asBoolean());
                    assertFalse(request.get().contains("local-test-key"));
                    assertEquals(new ModelId("served-model"), reply.modelId());
                    assertEquals(new PromptVersion("v1"), reply.promptVersion());
                    assertEquals(StandardSeverity.MINOR, reply.proposals().getFirst().estimatedSeverity());
                });
    }

    @Test
    void aRejectedTokenFailsWithoutRetryOrFallbackToTheStub() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/v1/chat/completions", exchange -> {
            calls.incrementAndGet();
            byte[] response = mapper.writeValueAsBytes(Map.of("error", Map.of("message", "Invalid key", "type", "authentication_error")));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        InspectionAssistant assistant = new SpringAiAssistantConfiguration().springAiInspectionAssistant(
                baseUrl(), "local-test-key", "test-model", "v1", Duration.ofSeconds(5), false, severities);
        assertThrows(AssistantUnavailableException.class, () -> assistant.review(inspection));
        assertEquals(1, calls.get());
    }

    @Test
    void theStubModeDoesNotRequireCredentialsOrCreateAnExternalClient() {
        contextRunner.withPropertyValues("certiflow.assistant.provider=stub")
                .run(context -> assertThat(context).doesNotHaveBean(InspectionAssistant.class));
    }

    @Test
    void selectingTheRealProviderWithoutCredentialsFailsAtStartup() {
        contextRunner.withPropertyValues("certiflow.assistant.provider=spring-ai")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "Configure certiflow.assistant.api-key before selecting spring-ai");
                });
    }

    @Test
    void timeoutMustBePositiveAndThePromptMustBePublished() {
        SpringAiAssistantConfiguration configuration = new SpringAiAssistantConfiguration();
        assertThrows(IllegalArgumentException.class, () -> configuration.springAiInspectionAssistant(
                baseUrl(), "local-test-key", "test-model", "v1", Duration.ZERO, false, severities));
        assertThrows(InvalidPromptException.class, () -> configuration.springAiInspectionAssistant(
                baseUrl(), "local-test-key", "test-model", "unpublished", Duration.ofSeconds(5), false, severities));
    }
}
