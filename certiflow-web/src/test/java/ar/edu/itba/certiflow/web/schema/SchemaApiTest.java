package ar.edu.itba.certiflow.web.schema;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Payloads;
import org.junit.jupiter.api.Test;

class SchemaApiTest extends ApiTest {

    @Test
    void creatingASchemaAndPublishingAVersionCreatesTheirResources() throws Exception {
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/schemas/Fire%20extinguisher")));

        postAs("EMP-001", "/api/v1/schemas/Fire extinguisher/versions", Payloads.version())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/schemas/Fire%20extinguisher/versions/2")))
                .andExpect(jsonPath("$.number").value(2));

        read("/api/v1/schemas/Fire extinguisher/versions/1")
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.sections[0].criteria[0].rule.minimum").value(6.00));
    }

    @Test
    void anUnknownRuleTypeIs400() throws Exception {
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema().replace("yes-no", "free-text"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aVersionThatDidNotChangeAnswers304ToItsETag() throws Exception {
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema());
        String etag = read("/api/v1/schemas/Fire extinguisher/versions/1").andReturn().getResponse().getHeader("ETag");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/schemas/Fire extinguisher/versions/1").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }
}
