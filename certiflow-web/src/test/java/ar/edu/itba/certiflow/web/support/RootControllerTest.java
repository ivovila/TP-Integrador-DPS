package ar.edu.itba.certiflow.web.support;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class RootControllerTest extends ApiTest {

    @Test
    void theEntryPointLinksEveryRootCollection() throws Exception {
        read("/api/v1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.assets.href").value("/api/v1/assets"))
                .andExpect(jsonPath("$._links.inspections.href").value("/api/v1/inspections"))
                .andExpect(jsonPath("$._links.certificates.href").value("/api/v1/certificates"))
                .andExpect(jsonPath("$._links.schema-list.href").value("/api/v1/schemas"))
                .andExpect(jsonPath("$._links.vocabulary.href").value("/api/v1/vocabulary"));
    }

    @Test
    void anUnsupportedMethodIs405() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/api/v1").with(as("EMP-001")))
                .andExpect(status().isMethodNotAllowed());
    }
}
