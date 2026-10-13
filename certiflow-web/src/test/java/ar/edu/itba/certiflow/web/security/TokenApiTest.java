package ar.edu.itba.certiflow.web.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.infrastructure.security.SpringDataUserAccountRepository;
import ar.edu.itba.certiflow.infrastructure.security.UserAccountEntity;
import ar.edu.itba.certiflow.web.support.ApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class TokenApiTest extends ApiTest {

    @Autowired
    private SpringDataUserAccountRepository userAccounts;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void registerUser() {
        userAccounts.save(new UserAccountEntity("EMP-002", "Ines Duarte", "INSPECTOR", passwordEncoder.encode("secret")));
    }

    @Test
    void logoutRevokesTheCurrentToken() throws Exception {
        String authorization = logIn();

        mvc.perform(delete("/api/v1/tokens/current").header("Authorization", authorization)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/assets").header("Authorization", authorization)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutLeavesOtherTokensOfThePersonValid() throws Exception {
        String revoked = logIn();
        String other = logIn();

        mvc.perform(delete("/api/v1/tokens/current").header("Authorization", revoked)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/assets").header("Authorization", other)).andExpect(status().isOk());
    }

    @Test
    void logoutWithBasicCredentialsIsABadRequest() throws Exception {
        mvc.perform(delete("/api/v1/tokens/current").with(httpBasic("EMP-002", "secret")))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"));
    }

    private String logIn() throws Exception {
        return mvc.perform(get("/api/v1").with(httpBasic("EMP-002", "secret")))
                .andReturn().getResponse().getHeader("Authorization");
    }
}
