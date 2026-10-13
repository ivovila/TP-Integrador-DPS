package ar.edu.itba.certiflow.web.security;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.infrastructure.security.SpringDataUserAccountRepository;
import ar.edu.itba.certiflow.web.support.ApiTest;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("demo-users")
class DemoUsersSeederTest extends ApiTest {

    @Autowired
    private DemoUsersSeeder seeder;
    @Autowired
    private SpringDataUserAccountRepository userAccounts;

    @Test
    void theDemoUsersCanLogInWithTheirRoleAndSeedingTwiceChangesNothing() throws Exception {
        String authorization = mvc.perform(get("/api/v1/assets").with(httpBasic("carlos.vidal", "certiflow")))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", startsWith("Bearer ")))
                .andReturn().getResponse().getHeader("Authorization");
        String payload = new String(Base64.getUrlDecoder().decode(authorization.substring("Bearer ".length()).split("\\.")[1]));

        seeder.run(null);

        assertEquals(true, payload.contains("\"role\":\"CERTIFICATION_OFFICER\""));
        assertEquals(true, payload.contains("\"name\":\"Carlos Vidal\""));
        assertEquals(6, userAccounts.count());
        assertEquals("EMP-006", userAccounts.findByUsername("ana.torres").orElseThrow().personId());
    }
}
