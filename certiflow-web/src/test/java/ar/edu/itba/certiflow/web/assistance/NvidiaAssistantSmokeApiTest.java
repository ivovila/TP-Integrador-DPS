package ar.edu.itba.certiflow.web.assistance;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Flows;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.ActiveProfiles;

/**
 * Una revisión de punta a punta con el modelo real del perfil {@code nvidia}: API, Spring AI, salida estructurada,
 * guardrails y persistencia. Necesita red y una key, así que solo corre con {@code NVIDIA_API_KEY} en el entorno;
 * en CI queda omitido. La respuesta queda en {@code target/nvidia-smoke-review.json} como evidencia.
 */
@ActiveProfiles("nvidia")
@EnabledIfEnvironmentVariable(named = "NVIDIA_API_KEY", matches = ".+")
class NvidiaAssistantSmokeApiTest extends ApiTest {

    @Test
    void theRealModelReviewsAnInspectionWhoseNotesContradictAnApprovedAnswer() throws Exception {
        new Flows(this).assetAndSchema();
        String inspection = Flows.path(postAs("COORDINATOR", "/api/v1/inspections", """
                {"assetCode":"EXT-0042","inspector":{"id":"INSPECTOR","name":"Ines"},"plannedDate":"2026-12-01"}
                """).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location"));
        putAs("INSPECTOR", inspection + "/responses/EXT-02/answer", """
                {"value":true}
                """).andExpect(status().isNoContent());
        postAs("INSPECTOR", inspection + "/responses/EXT-02/observations", """
                {"text":"El precinto está roto y falta el alambre de seguridad"}
                """).andExpect(status().isCreated());

        String review = postAs("INSPECTOR", inspection + "/assistant-reviews", "")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.promptVersion").value("v1"))
                .andExpect(jsonPath("$.suggestions[?(@.criterionCode == 'EXT-02')]").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        Files.writeString(Path.of("target", "nvidia-smoke-review.json"), review);
    }
}
