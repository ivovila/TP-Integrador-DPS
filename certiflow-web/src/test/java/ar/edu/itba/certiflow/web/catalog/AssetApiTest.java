package ar.edu.itba.certiflow.web.catalog;

import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Payloads;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AssetApiTest extends ApiTest {

    @Test
    void registeringAnAssetCreatesItsResourceAndItsHistory() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/assets/EXT-0042")))
                .andExpect(jsonPath("$.code").value("EXT-0042"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/api/v1/assets/EXT-0042")))
                .andExpect(jsonPath("$._links.findings-summary.href").exists())
                .andExpect(jsonPath("$._links.audit-entries.href").exists());

        read("/api/v1/assets/EXT-0042/audit-entries")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.auditEntries[0].action").value("REGISTERED"))
                .andExpect(jsonPath("$._embedded.auditEntries[0].performedById").value("EMP-001"));
    }

    @Test
    void registeringTheSameCodeTwiceIs409() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset());

        postAs("EMP-001", "/api/v1/assets", Payloads.asset())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("AssetAlreadyRegisteredException"));
    }

    @Test
    void anUnknownAssetIs404AsProblemJson() throws Exception {
        read("/api/v1/assets/NOPE")
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"));
    }

    @Test
    void writingWithoutTheActingPersonIs400() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/assets")
                        .contentType(MediaType.APPLICATION_JSON).content(Payloads.asset()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        postAs("EMP-001", "/api/v1/assets", "{\"code\":").andExpect(status().isBadRequest());
    }

    @Test
    void assetsAreListedByTypeInPages() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset());
        postAs("EMP-001", "/api/v1/assets", Payloads.asset().replace("EXT-0042", "EXT-0043"));

        read("/api/v1/assets?type=Fire extinguisher&page=0&size=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.assets.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.prev").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=0&size=0", "page=-1&size=10", "page=0&size=-5"})
    void invalidPagingIs400(String query) throws Exception {
        read("/api/v1/assets?" + query).andExpect(status().isBadRequest());
    }

    @Test
    void aValueTooLongForStorageIs400() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset().replace("Lobby extinguisher", "x".repeat(300)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"));
    }

    @Test
    void linksEncodeValuesWithReservedCharacters() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset().replace("Fire extinguisher", "Boiler & \\\"Steam\\\""));
        postAs("EMP-001", "/api/v1/assets", Payloads.asset().replace("EXT-0042", "EXT-0043").replace("Fire extinguisher", "Boiler & \\\"Steam\\\""));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/assets")
                        .param("type", "Boiler & \"Steam\"").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.assets.length()").value(1))
                .andExpect(jsonPath("$._links.next.href").value(org.hamcrest.Matchers.containsString("type=Boiler%20%26%20%22Steam%22")));
    }
}
