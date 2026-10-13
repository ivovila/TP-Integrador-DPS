package ar.edu.itba.certiflow.web.inspection;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Payloads;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class InspectionApiTest extends ApiTest {

    private static final String ASSIGNMENT = """
            {"assetCode":"EXT-0042","inspector":{"id":"EMP-002","name":"Ines"},"plannedDate":"2026-12-01","sections":null}""";

    @Autowired
    private JdbcTemplate jdbc;

    private String inspection;

    @BeforeEach
    void assignInspection() throws Exception {
        postAs("EMP-001", "/api/v1/assets", Payloads.asset());
        postAs("EMP-001", "/api/v1/schemas", Payloads.schema());
        String location = postAs("EMP-001", "/api/v1/inspections", ASSIGNMENT)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        inspection = location.substring(location.indexOf("/api/v1/inspections/"));
    }

    private void answerAll(String pressure) throws Exception {
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":" + pressure + "}").andExpect(status().isNoContent());
        postAs("EMP-002", inspection + "/responses/EXT-01/evidence",
                "{\"kind\":\"PHOTO\",\"reference\":\"photos/gauge.jpg\",\"description\":\"Gauge\"}").andExpect(status().isCreated());
        putAs("EMP-002", inspection + "/responses/EXT-02/answer", "{\"value\":true}").andExpect(status().isNoContent());
        putAs("EMP-002", inspection + "/responses/EXT-03/answer", "{\"value\":\"VISIBLE\"}").andExpect(status().isNoContent());
    }

    @Test
    void anOpenInspectionOffersItsResponsesAndClosureButNotRectifications() throws Exception {
        read(inspection)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$._links.responses.href").exists())
                .andExpect(jsonPath("$._links.closure.href").exists())
                .andExpect(jsonPath("$._links.rectifications").doesNotExist())
                .andExpect(jsonPath("$._links.act").doesNotExist());
    }

    @Test
    void answeringIsIdempotentAndKeepsTheDecimalScale() throws Exception {
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":7.00}").andExpect(status().isNoContent());
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":7.00}").andExpect(status().isNoContent());

        read(inspection + "/responses/EXT-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(7.00))
                .andExpect(jsonPath("$.outcome").value("PENDING"));
    }

    @Test
    void closingCreatesTheFirstRevisionAndLinksTheFindingsItRaised() throws Exception {
        answerAll("5.00");

        postAs("EMP-002", inspection + "/closure", "{}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(inspection + "/revisions/1")))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$._links.findings").exists());

        read(inspection)
                .andExpect(jsonPath("$.closed").value(true))
                .andExpect(jsonPath("$._links.closure").doesNotExist())
                .andExpect(jsonPath("$._links.rectifications.href").exists())
                .andExpect(jsonPath("$._links.act.href").exists());
        read(inspection + "/revisions/1").andExpect(status().isOk()).andExpect(header().exists("ETag"));
    }

    @Test
    void closingWithPendingCriteriaIs409AndLeavesNoAuditTrace() throws Exception {
        Integer before = jdbc.queryForObject("select count(*) from audit_entry", Integer.class);

        postAs("EMP-002", inspection + "/closure", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("InspectionIncompleteException"));

        Assertions.assertEquals(before, jdbc.queryForObject("select count(*) from audit_entry", Integer.class));
    }

    @Test
    void anAnswerOfTheWrongTypeIs400() throws Exception {
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":\"abc\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("AnswerTypeMismatchException"));
        putAs("EMP-002", inspection + "/responses/EXT-02/answer", "{\"value\":5}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownCriterionCodeIs404() throws Exception {
        putAs("EMP-002", inspection + "/responses/EXT-99/answer", "{\"value\":true}").andExpect(status().isNotFound());
    }

    @Test
    void rectifyingCreatesTheNextRevisionAndTheActIsAvailable() throws Exception {
        answerAll("7.00");
        postAs("EMP-002", inspection + "/closure", "{}");

        postAs("EMP-001", inspection + "/rectifications", "{\"criterionCode\":\"EXT-03\",\"value\":\"REFLECTIVE\",\"reason\":\"Typo\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(inspection + "/revisions/2")));

        read(inspection + "/act")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(3))
                .andExpect(jsonPath("$.lines[0].answer").value(7.00));
        read(inspection + "/revisions").andExpect(jsonPath("$._embedded.revisions.length()").value(2));
    }

    @Test
    void theActOfAnOpenInspectionIs409() throws Exception {
        read(inspection + "/act").andExpect(status().isConflict());
    }

    @Test
    void eligibilityExplainsWhyAnOpenInspectionCannotBeCertified() throws Exception {
        read(inspection + "/certification-eligibility")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.ineligibilities[0].reason").value("INSPECTION_NOT_CLOSED"));
    }

    @Test
    void historyListsEveryStep() throws Exception {
        answerAll("7.00");

        read(inspection + "/audit-entries").andExpect(jsonPath("$._embedded.auditEntries.length()").value(5));
    }

    @Test
    void aFailureAfterTheAuditEntryIsWrittenRollsTheEntryBack() throws Exception {
        Integer before = jdbc.queryForObject("select count(*) from audit_entry", Integer.class);

        postAs("EMP-002", inspection + "/responses/EXT-01/evidence",
                "{\"kind\":\"PHOTO\",\"reference\":\"" + "r".repeat(600) + "\",\"description\":\"G\"}")
                .andExpect(status().isBadRequest());

        Assertions.assertEquals(before, jdbc.queryForObject("select count(*) from audit_entry", Integer.class));
    }

    @Test
    void theDecimalScaleReachesTheClient() throws Exception {
        putAs("EMP-002", inspection + "/responses/EXT-01/answer", "{\"value\":7.00}");

        read(inspection + "/responses/EXT-01").andExpect(content().string(containsString("\"answer\":7.00")));
    }

    @Test
    void inspectionsAreListedByAssetAndStatusInPages() throws Exception {
        answerAll("7.00");
        postAs("EMP-002", inspection + "/closure", "{}");
        postAs("EMP-001", "/api/v1/inspections", ASSIGNMENT);

        read("/api/v1/inspections?asset=EXT-0042&page=0&size=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.inspections.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        read("/api/v1/inspections?status=closed").andExpect(jsonPath("$._embedded.inspections.length()").value(1))
                .andExpect(jsonPath("$._embedded.inspections[0].closed").value(true));
        read("/api/v1/inspections?status=open").andExpect(jsonPath("$._embedded.inspections[0].closed").value(false));
        read("/api/v1/inspections?status=archived").andExpect(status().isBadRequest());
    }

    @Test
    void aMissingRevisionIs404AsProblemJson() throws Exception {
        read(inspection + "/revisions/9")
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"));
    }

    @Test
    void answeringACriterionOutsideTheScopeIs404LikeReadingIt() throws Exception {
        String scoped = postAs("EMP-001", "/api/v1/inspections", ASSIGNMENT.replace("\"sections\":null", "\"sections\":[\"Signage\"]"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String path = scoped.substring(scoped.indexOf("/api/v1/inspections/"));

        read(path + "/responses/EXT-01").andExpect(status().isNotFound());
        putAs("EMP-002", path + "/responses/EXT-01/answer", "{\"value\":7.00}").andExpect(status().isNotFound());
    }

    @Test
    void evidenceAndObservationsAreResourcesOfTheirOwn() throws Exception {
        String evidence = postAs("EMP-002", inspection + "/responses/EXT-01/evidence",
                        "{\"kind\":\"PHOTO\",\"reference\":\"photos/gauge.jpg\",\"description\":\"Gauge\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(inspection + "/responses/EXT-01/evidence/1")))
                .andReturn().getResponse().getHeader("Location");
        String observation = postAs("EMP-002", inspection + "/responses/EXT-01/observations", "{\"text\":\"Needle trembles\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(inspection + "/responses/EXT-01/observations/1")))
                .andReturn().getResponse().getHeader("Location");

        read(evidence.substring(evidence.indexOf("/api/v1"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("photos/gauge.jpg"))
                .andExpect(jsonPath("$._links.response.href").exists());
        read(observation.substring(observation.indexOf("/api/v1"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Needle trembles"));
        read(inspection + "/responses/EXT-01/evidence/7").andExpect(status().isNotFound());
    }
}
