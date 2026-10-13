package ar.edu.itba.certiflow.web.schema;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Payloads;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/schemas/Fire extinguisher/versions/1").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"rule\":{\"type\":\"numeric-range\",\"minimum\":6.00,\"maximum\":8.00}",
            "\"rule\":{\"type\":\"numeric-range\",\"unit\":\"bar\"}",
            "\"rule\":{\"type\":\"yes-no\",\"expected\":\"MAYBE\"}",
            "\"rule\":{\"type\":\"yes-no\"}",
            "\"rule\":{\"type\":\"option-in-list\"}"})
    void anIncompleteRuleIs400(String rule) throws Exception {
        String schema = "{\"name\":\"S\",\"assetType\":\"Boiler\",\"sections\":[{\"title\":\"T\",\"criteria\":["
                + "{\"code\":\"B-1\",\"text\":\"Check\",\"severity\":\"MAJOR\"," + rule + "}]}]}";

        postAs("EMP-001", "/api/v1/schemas", schema).andExpect(status().isBadRequest());
    }

    @Test
    void aNullSectionIs400() throws Exception {
        postAs("EMP-001", "/api/v1/schemas", "{\"name\":\"S\",\"assetType\":\"Boiler\",\"sections\":[null]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void aMissingVersionIs404AsProblemJson() throws Exception {
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema());

        read("/api/v1/schemas/Fire extinguisher/versions/9")
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"));
    }

    @Test
    void theETagIsAValidHeaderWhateverTheAssetType() throws Exception {
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema().replace("Fire extinguisher", "Caldera \\\"Vapor\\\" ñ"));

        mvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/schemas/{type}/versions/1", "Caldera \"Vapor\" ñ"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", Matchers.matchesPattern("\"[A-Za-z0-9-]+\"")));
    }
}
