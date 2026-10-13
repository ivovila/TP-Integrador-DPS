package ar.edu.itba.certiflow.web.config;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("demo")
class DemoDataSeederTest extends ApiTest {

    @Test
    void theDemoProfileLoadsAssetsUnderEveryJurisdictionWithPartialAndGlobalCertificates() throws Exception {
        read("/api/v1/assets").andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.assets.length()").value(4));
        read("/api/v1/schemas/Fire extinguisher")
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andExpect(jsonPath("$.latestPublishedVersion").value(2));
        read("/api/v1/certificates?asset=BLR-0007")
                .andExpect(jsonPath("$._embedded.certificates[2].basis.type").value("PARTIAL_CERTIFICATES"))
                .andExpect(jsonPath("$._embedded.certificates[2].status").value("DEPENDENCY_INVALID"));
        read("/api/v1/certificates?asset=BLR-0012")
                .andExpect(jsonPath("$._embedded.certificates[1].decision.outcome").value("CONDITIONAL"))
                .andExpect(jsonPath("$._embedded.certificates[2].decision.outcome").value("CONDITIONAL"));
    }
}
