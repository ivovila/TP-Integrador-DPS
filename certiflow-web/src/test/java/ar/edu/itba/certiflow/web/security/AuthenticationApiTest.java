package ar.edu.itba.certiflow.web.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.infrastructure.security.SpringDataUserAccountRepository;
import ar.edu.itba.certiflow.infrastructure.security.UserAccountEntity;
import ar.edu.itba.certiflow.web.support.ApiTest;
import ar.edu.itba.certiflow.web.support.Payloads;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class AuthenticationApiTest extends ApiTest {

    @Autowired
    private SpringDataUserAccountRepository userAccounts;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtEncoder jwtEncoder;

    @BeforeEach
    void registerUsers() {
        userAccounts.save(new UserAccountEntity("EMP-002", "Ines Duarte", "INSPECTOR", passwordEncoder.encode("secret")));
        userAccounts.save(new UserAccountEntity("EMP-006", "Ana Torres", "ADMINISTRATOR", passwordEncoder.encode("secret")));
    }

    @Test
    void basicCredentialsOnAnyPathReturnABearerToken() throws Exception {
        String authorization = mvc.perform(get("/api/v1/assets").with(httpBasic("EMP-002", "secret")))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", startsWith("Bearer ")))
                .andExpect(header().exists("Token-Expires"))
                .andReturn().getResponse().getHeader("Authorization");

        mvc.perform(get("/api/v1/assets").header("Authorization", authorization)).andExpect(status().isOk());
    }

    @Test
    void aFailingRequestStillReturnsTheToken() throws Exception {
        mvc.perform(get("/api/v1/assets/NOPE").with(httpBasic("EMP-002", "secret")))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Authorization", startsWith("Bearer ")));
    }

    @Test
    void aWrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/assets").with(httpBasic("EMP-002", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(header().doesNotExist("Authorization"));
    }

    @Test
    void anUnknownPersonIsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/assets").with(httpBasic("EMP-404", "secret")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noCredentialsAreChallengedWithBasic() throws Exception {
        mvc.perform(get("/api/v1/assets"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Basic")))
                .andExpect(header().string("Content-Type", "application/problem+json"));
    }

    @Test
    void browserRequestsAreNotChallengedWithBasic() throws Exception {
        mvc.perform(get("/api/v1/assets").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void anExpiredTokenIsUnauthorized() throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(7200);
        String token = signed(jwtEncoder, issuedAt, issuedAt.plusSeconds(3600));

        mvc.perform(get("/api/v1/assets").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenSignedWithAnotherKeyIsUnauthorized() throws Exception {
        JwtEncoder anotherKey = NimbusJwtEncoder.withSecretKey(
                new SecretKeySpec("another-key-that-is-at-least-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256")).build();
        String token = signed(anotherKey, Instant.now(), Instant.now().plusSeconds(3600));

        mvc.perform(get("/api/v1/assets").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theTokenIdentifiesTheActingPerson() throws Exception {
        String authorization = mvc.perform(get("/api/v1").with(httpBasic("EMP-006", "secret")))
                .andReturn().getResponse().getHeader("Authorization");

        mvc.perform(post("/api/v1/assets").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(Payloads.asset()))
                .andExpect(status().isCreated());

        read("/api/v1/assets/EXT-0042/audit-entries")
                .andExpect(jsonPath("$._embedded.auditEntries[0].performedById").value("EMP-006"))
                .andExpect(jsonPath("$._embedded.auditEntries[0].performedByName").value("Ana Torres"));
    }

    private static String signed(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("EMP-002").claim("name", "Ines Duarte").claim("role", "INSPECTOR")
                .id(UUID.randomUUID().toString()).issuedAt(issuedAt).expiresAt(expiresAt).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
