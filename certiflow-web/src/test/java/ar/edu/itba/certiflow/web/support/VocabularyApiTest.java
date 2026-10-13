package ar.edu.itba.certiflow.web.support;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class VocabularyApiTest extends ApiTest {

    @Test
    void theVocabularyListsSeveritiesWithTheOutcomeOfNotMeetingThem() throws Exception {
        read("/api/v1/vocabulary")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severities[*].name").value(contains("MINOR", "MAJOR", "CRITICAL")))
                .andExpect(jsonPath("$.severities[0].label").value("Minor"))
                .andExpect(jsonPath("$.severities[*].outcomeWhenUnmet").value(contains("OBSERVED", "REJECTED", "REJECTED")));
    }

    @Test
    void theVocabularyListsEvidenceKindsByNameAndLabel() throws Exception {
        read("/api/v1/vocabulary")
                .andExpect(jsonPath("$.evidenceKinds[*].name").value(contains("PHOTO", "DOCUMENT")))
                .andExpect(jsonPath("$.evidenceKinds[*].label").value(contains("Photo", "Document")));
    }

    @Test
    void theVocabularyListsEveryJurisdictionWithARegisteredPolicy() throws Exception {
        read("/api/v1/vocabulary")
                .andExpect(jsonPath("$.jurisdictions[*].code").value(contains("DEMO-CONDITIONAL", "DEMO-STANDARD", "DEMO-STRICT")))
                .andExpect(jsonPath("$.jurisdictions[*].policyCode").value(contains("CONDITIONAL", "STANDARD", "STRICT")));
    }
}
