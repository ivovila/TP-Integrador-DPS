package ar.edu.itba.certiflow.web;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Payloads;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EndToEndApiTest extends ApiTest {

    private String href(String json, String path) {
        String href = JsonPath.read(json, path);
        return href.substring(href.indexOf("/api/v1"));
    }

    private String inspectionAnswered(String pressure) throws Exception {
        String assigned = postAs("EMP-001", "/api/v1/inspections", """
                {"assetCode":"EXT-0042","inspector":{"id":"EMP-002","name":"Ines"},"plannedDate":"2026-12-01","sections":null}""")
                .andReturn().getResponse().getContentAsString();
        String inspection = href(assigned, "$._links.self.href");
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":" + pressure + "}");
        postAs("EMP-002", inspection + "/responses/EXT-01/evidence", "{\"kind\":\"PHOTO\",\"reference\":\"g.jpg\",\"description\":\"G\"}");
        putAs("EMP-002", inspection + "/responses/EXT-02/answer", "{\"value\":true}");
        putAs("EMP-002", inspection + "/responses/EXT-03/answer", "{\"value\":\"VISIBLE\"}");
        return inspection;
    }

    @Test
    void anAssetGoesFromInspectionToCertificateFollowingTheLinks() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset());
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema());
        String inspection = inspectionAnswered("5.00");

        String closure = postAs("EMP-002", inspection + "/closure", "{}").andReturn().getResponse().getContentAsString();
        String finding = href(closure, "$._links.findings.href");

        read(inspection + "/certification-eligibility")
                .andExpect(jsonPath("$.ineligibilities[0].reason").value("BLOCKING_FINDINGS"));
        postAs("EMP-006", "/api/v1/certificates", "{\"inspectionId\":\"" + inspection.substring(inspection.lastIndexOf('/') + 1)
                + "\",\"validUntil\":\"" + LocalDate.now().plusYears(1) + "\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.ineligibilities[0].reason").value("BLOCKING_FINDINGS"));

        String findingJson = read(finding).andExpect(jsonPath("$.severity").value("MAJOR")).andReturn().getResponse().getContentAsString();
        String action = postAs("EMP-001", href(findingJson, "$._links.corrective-actions.href"),
                "{\"description\":\"Recharge\",\"responsible\":{\"id\":\"EMP-004\",\"name\":\"Marta\"},\"dueDate\":\""
                        + LocalDate.now().plusDays(10) + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        postAs("EMP-005", action.substring(action.indexOf("/api/v1")) + "/verifications",
                "{\"result\":\"ACCEPTED\",\"notes\":\"Recharged\"}")
                .andExpect(status().isCreated());
        read(finding).andExpect(jsonPath("$.open").value(false)).andExpect(jsonPath("$._links.corrective-actions").doesNotExist());

        String second = inspectionAnswered("7.00");
        postAs("EMP-002", second + "/closure", "{}");
        String certificate = postAs("EMP-006", "/api/v1/certificates", "{\"inspectionId\":\""
                + second.substring(second.lastIndexOf('/') + 1) + "\",\"validUntil\":\"" + LocalDate.now().plusYears(1) + "\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/certificates/CERT-00001")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$._links.suspensions.href").exists())
                .andReturn().getResponse().getHeader("Location");

        read("/api/v1/assets/EXT-0042/findings-summary").andExpect(status().isOk());
        read(certificate.substring(certificate.indexOf("/api/v1")) + "/audit-entries")
                .andExpect(jsonPath("$._embedded.auditEntries[0].action").value("ISSUED"));
    }
}
