package ar.edu.itba.certiflow.web.support;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class RootControllerTest extends ApiTest {

    @Test
    void theEntryPointLinksEveryRootCollection() throws Exception {
        read("/api/v1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.assets.href").value("/api/v1/assets"))
                .andExpect(jsonPath("$._links.inspections.href").value("/api/v1/inspections"))
                .andExpect(jsonPath("$._links.certificates.href").value("/api/v1/certificates"));
    }

    @Test
    void anUnsupportedMethodIs405() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1"))
                .andExpect(status().isMethodNotAllowed());
    }
}
