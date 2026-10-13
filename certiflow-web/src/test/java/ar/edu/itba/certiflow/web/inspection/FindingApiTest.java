package ar.edu.itba.certiflow.web.inspection;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Flows;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FindingApiTest extends ApiTest {

    private final Flows flows = new Flows(this);
    private String finding;

    @BeforeEach
    void raiseAMajorFinding() throws Exception {
        flows.assetAndSchema();
        finding = flows.findingOf(flows.closedInspection("5.00"));
    }

    private String action(String responsibleId, LocalDate dueDate) {
        return "{\"description\":\"Recharge\",\"responsible\":{\"id\":\"" + responsibleId + "\",\"name\":\"Marta\"},\"dueDate\":\""
                + dueDate + "\"}";
    }

    @Test
    void findingsAreListedByAssetInPages() throws Exception {
        read("/api/v1/findings?asset=EXT-0042&page=0&size=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.findings[0].severity").value("MAJOR"))
                .andExpect(jsonPath("$._embedded.findings[0].open").value(true))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void anOpenFindingOffersCorrectiveActions() throws Exception {
        read(finding).andExpect(jsonPath("$._links.corrective-actions.href").exists());
    }

    @Test
    void aDueDateInThePastIs409() throws Exception {
        postAs("EMP-001", finding + "/corrective-actions", action("EMP-004", LocalDate.now().minusDays(5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("DueDateInThePastException"));
    }

    @Test
    void theResponsibleOfAnActionCannotVerifyIt() throws Exception {
        String location = postAs("EMP-001", finding + "/corrective-actions", action("EMP-004", LocalDate.now().plusDays(5)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        postAs("EMP-004", Flows.path(location) + "/verifications", "{\"result\":\"ACCEPTED\",\"notes\":\"Done\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("VerifierMustDifferFromResponsibleException"));
    }

    @Test
    void aRejectedVerificationWithoutNotesIs409() throws Exception {
        String location = postAs("EMP-001", finding + "/corrective-actions", action("EMP-004", LocalDate.now().plusDays(5)))
                .andReturn().getResponse().getHeader("Location");

        postAs("EMP-005", Flows.path(location) + "/verifications", "{\"result\":\"REJECTED\",\"notes\":\"\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("VerificationNotesRequiredException"));
    }

    @Test
    void anUnknownActionIs404() throws Exception {
        read(finding + "/corrective-actions/9").andExpect(status().isNotFound());
    }

    @Test
    void theFindingsSummaryUsesPlainValues() throws Exception {
        read("/api/v1/assets/EXT-0042/findings-summary")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetCode").value("EXT-0042"))
                .andExpect(jsonPath("$.findings[0].responsible.id").value("EMP-003"))
                .andExpect(jsonPath("$.openFindings").value(1));
    }

    @Test
    void aVerificationIsAResourceOfItsOwn() throws Exception {
        String action = Flows.path(postAs("EMP-001", finding + "/corrective-actions", action("EMP-004", LocalDate.now().plusDays(5)))
                .andReturn().getResponse().getHeader("Location"));

        String location = postAs("EMP-005", action + "/verifications", "{\"result\":\"REJECTED\",\"notes\":\"Still low\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(action + "/verifications/1")))
                .andReturn().getResponse().getHeader("Location");

        read(Flows.path(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("REJECTED"))
                .andExpect(jsonPath("$._links.corrective-action.href").exists());
    }
}
