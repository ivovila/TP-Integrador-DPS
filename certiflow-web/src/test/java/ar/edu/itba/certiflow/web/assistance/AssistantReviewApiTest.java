package ar.edu.itba.certiflow.web.assistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Flows;
import org.junit.jupiter.api.Test;

/** Contrato HAL, persistencia y decisiones sobre una inspección real; el asistente es el stub sin red. */
public class AssistantReviewApiTest extends ApiTest {

    private String openInspection() throws Exception {
        new Flows(this).assetAndSchema();
        return Flows.path(postAs("COORDINATOR", "/api/v1/inspections", """
                {"assetCode":"EXT-0042","inspector":{"id":"INSPECTOR","name":"Ines"},"plannedDate":"2026-12-01"}
                """).andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location"));
    }

    private String reviewWithTwoSuggestions(String inspection) throws Exception {
        putAs("INSPECTOR", inspection + "/responses/EXT-02/answer", """
                {"value":true}
                """).andExpect(status().isNoContent());
        postAs("INSPECTOR", inspection + "/responses/EXT-02/observations", """
                {"text":"El precinto está roto"}
                """)
                .andExpect(status().isCreated());
        postAs("INSPECTOR", inspection + "/responses/EXT-03/evidence",
                """
                {"kind":"PHOTO","reference":"sign.jpg","description":"Una caja tapa el cartel"}
                """)
                .andExpect(status().isCreated());
        return Flows.path(postAs("INSPECTOR", inspection + "/assistant-reviews", "")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.suggestions.length()").value(2))
                .andExpect(jsonPath("$.promptVersion").value("stub"))
                .andExpect(jsonPath("$.modelId").value("keyword-stub"))
                .andExpect(jsonPath("$.requestedBy.id").value("INSPECTOR"))
                .andReturn().getResponse().getHeader("Location"));
    }

    @Test
    void aHumanDecidesOnceAndTheInspectionDoesNotChange() throws Exception {
        String inspection = openInspection();
        String review = reviewWithTwoSuggestions(inspection);
        String before = read(inspection + "/responses").andReturn().getResponse().getContentAsString();
        read(review).andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].criterionSeverity").value("CRITICAL"))
                .andExpect(jsonPath("$.suggestions[0].estimatedSeverity").value("MAJOR"))
                .andExpect(jsonPath("$.suggestions[0].quotes[0]").value("El precinto está roto"))
                .andExpect(jsonPath("$.suggestions[0]._links.criterion.href").exists())
                .andExpect(jsonPath("$.suggestions[0]._links.acceptance.href").exists())
                .andExpect(jsonPath("$.suggestions[0]._links.dismissal.href").exists());
        read(review + "/suggestions/1/acceptance").andExpect(status().isNotFound());
        String accepted = postAs("HUMAN", review + "/suggestions/1/acceptance", "")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.decidedBy.id").value("HUMAN"))
                .andReturn().getResponse().getHeader("Location");
        read(Flows.path(accepted)).andExpect(status().isOk()).andExpect(jsonPath("$.decidedAt").exists());
        read(review + "/suggestions/1").andExpect(jsonPath("$.decision.status").value("ACCEPTED"))
                .andExpect(jsonPath("$._links.acceptance").doesNotExist()).andExpect(jsonPath("$._links.dismissal").doesNotExist());
        postAs("HUMAN", review + "/suggestions/1/dismissal", """
                {"reason":"Changed my mind"}
                """)
                .andExpect(status().isConflict()).andExpect(content().contentType("application/problem+json"));
        String dismissed = postAs("HUMAN", review + "/suggestions/2/dismissal", """
                {"reason":"Se retiró la caja"}
                """)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.reason").value("Se retiró la caja"))
                .andReturn().getResponse().getHeader("Location");
        read(Flows.path(dismissed)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISMISSED"));
        read(review + "/suggestions/2/acceptance").andExpect(status().isNotFound());
        postAs("HUMAN", review + "/suggestions/2/acceptance", "").andExpect(status().isConflict());
        read(review + "/audit-entries").andExpect(jsonPath("$._embedded.auditEntries.length()").value(3));
        read(inspection + "/assistant-reviews").andExpect(jsonPath("$._embedded['assistant-reviews'].length()").value(1));
        assertEquals(before, read(inspection + "/responses").andReturn().getResponse().getContentAsString());
        read(inspection).andExpect(jsonPath("$.closed").value(false));
    }

    @Test
    void invalidDecisionsAndMissingResourcesReturnProblemsWithoutChangingTheReview() throws Exception {
        String review = reviewWithTwoSuggestions(openInspection());
        for (String body : new String[] {"""
                {}
                """, """
                {"reason":null}
                """, """
                {"reason":" "}
                """}) {
            postAs("HUMAN", review + "/suggestions/1/dismissal", body).andExpect(status().isBadRequest())
                    .andExpect(header().string("Content-Type", "application/problem+json"));
        }
        postAs("HUMAN", review + "/suggestions/0/acceptance", "").andExpect(status().isNotFound());
        read(review + "/suggestions/99").andExpect(status().isNotFound());
        read("/api/v1/assistant-reviews/missing").andExpect(status().isNotFound());
        read("/api/v1/assistant-reviews/missing/audit-entries").andExpect(status().isNotFound());
        read("/api/v1/inspections/missing/assistant-reviews").andExpect(status().isNotFound());
        read(review + "/suggestions/1").andExpect(jsonPath("$.decision.status").value("PENDING"));
        read(review + "/audit-entries").andExpect(jsonPath("$._embedded.auditEntries.length()").value(1));
    }

    @Test
    void guardrailsKeepAlreadyNonconformingProposalsOutOfSuggestions() throws Exception {
        String inspection = openInspection();
        putAs("INSPECTOR", inspection + "/responses/EXT-02/answer", """
                {"value":false}
                """);
        postAs("INSPECTOR", inspection + "/responses/EXT-02/observations", """
                {"text":"El precinto está roto"}
                """);
        String review = Flows.path(postAs("INSPECTOR", inspection + "/assistant-reviews", "")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.suggestions.length()").value(0))
                .andExpect(jsonPath("$.rejectedProposals[0].reason").value("ALREADY_NONCONFORMING"))
                .andReturn().getResponse().getHeader("Location"));
        postAs("INSPECTOR", review + "/suggestions/1/acceptance", "").andExpect(status().isNotFound());
    }

    @Test
    void closedInspectionsKeepTheirHistoryButRejectNewReviews() throws Exception {

        String inspection = openInspection();
        String review = reviewWithTwoSuggestions(inspection);
        putAs("INSPECTOR", inspection + "/responses/EXT-01/answer", """
                {"value":7}
                """).andExpect(status().isNoContent());
        postAs("INSPECTOR", inspection + "/responses/EXT-01/evidence", """
                {"kind":"PHOTO","reference":"gauge.jpg","description":"Gauge in range"}
                """).andExpect(status().isCreated());
        putAs("INSPECTOR", inspection + "/responses/EXT-03/answer", """
                {"value":"VISIBLE"}
                """).andExpect(status().isNoContent());
        postAs("INSPECTOR", inspection + "/closure", "").andExpect(status().isCreated());
        read(inspection).andExpect(jsonPath("$._links['assistant-reviews'].href").exists());
        postAs("INSPECTOR", inspection + "/assistant-reviews", "").andExpect(status().isConflict())
                .andExpect(content().contentType("application/problem+json"));
        read(inspection + "/assistant-reviews").andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded['assistant-reviews'].length()").value(1));
        read(review + "/suggestions/1").andExpect(jsonPath("$.decision.status").value("PENDING"));
        postAs("HUMAN", review + "/suggestions/1/acceptance", "").andExpect(status().isCreated());
        read(inspection).andExpect(jsonPath("$.closed").value(true));
    }
}
