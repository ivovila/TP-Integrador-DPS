package ar.edu.itba.certiflow.web.assistance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.assistance.model.InspectionAssistant;
import ar.edu.itba.certiflow.assistance.model.exceptions.AssistantUnavailableException;
import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Flows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Import(AssistantUnavailableApiTest.UnavailableProvider.class)
class AssistantUnavailableApiTest extends ApiTest {

    @Autowired
    private JdbcTemplate jdbc;

    @TestConfiguration
    static class UnavailableProvider {
        @Bean
        @Primary
        InspectionAssistant unavailableAssistant() {
            return inspection -> {
                assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
                throw new AssistantUnavailableException("Provider unavailable");
            };
        }
    }

    @Test
    void aProviderFailureIs503AndLeavesNeitherAReviewNorAnAuditEntry() throws Exception {
        new Flows(this).assetAndSchema();
        String inspection = Flows.path(postAs("COORDINATOR", "/api/v1/inspections", """
                {"assetCode":"EXT-0042","inspector":{"id":"INSPECTOR","name":"Ines"},"plannedDate":"2026-12-01"}
                """).andReturn().getResponse().getHeader("Location"));
        postAs("INSPECTOR", inspection + "/assistant-reviews", "").andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType("application/problem+json"));
        read(inspection + "/assistant-reviews").andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject("select count(*) from assistant_review", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                jdbc.queryForObject("select count(*) from audit_entry where subject_type = 'assistance'", Integer.class));
    }
}
