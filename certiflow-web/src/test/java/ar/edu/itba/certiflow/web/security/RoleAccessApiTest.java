package ar.edu.itba.certiflow.web.security;

import static ar.edu.itba.certiflow.web.security.StaffRole.ADMINISTRATOR;
import static ar.edu.itba.certiflow.web.security.StaffRole.ASSET_RESPONSIBLE;
import static ar.edu.itba.certiflow.web.security.StaffRole.CERTIFICATION_OFFICER;
import static ar.edu.itba.certiflow.web.security.StaffRole.INSPECTION_COORDINATOR;
import static ar.edu.itba.certiflow.web.security.StaffRole.INSPECTOR;
import static ar.edu.itba.certiflow.web.security.StaffRole.VERIFIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.itba.certiflow.web.support.ApiTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class RoleAccessApiTest extends ApiTest {

    static Stream<Arguments> writes() {
        return Stream.of(
                Arguments.of(POST, "/api/v1/assets", ADMINISTRATOR),
                Arguments.of(POST, "/api/v1/schemas", ADMINISTRATOR),
                Arguments.of(POST, "/api/v1/schemas/T/versions", ADMINISTRATOR),
                Arguments.of(POST, "/api/v1/inspections", INSPECTION_COORDINATOR),
                Arguments.of(PUT, "/api/v1/inspections/I/responses/C/answer", INSPECTOR),
                Arguments.of(POST, "/api/v1/inspections/I/responses/C/evidence", INSPECTOR),
                Arguments.of(POST, "/api/v1/inspections/I/responses/C/observations", INSPECTOR),
                Arguments.of(POST, "/api/v1/inspections/I/closure", INSPECTOR),
                Arguments.of(POST, "/api/v1/inspections/I/rectifications", INSPECTOR),
                Arguments.of(POST, "/api/v1/findings/F/corrective-actions", ASSET_RESPONSIBLE),
                Arguments.of(POST, "/api/v1/findings/F/corrective-actions/1/verifications", VERIFIER),
                Arguments.of(POST, "/api/v1/certificates", CERTIFICATION_OFFICER),
                Arguments.of(POST, "/api/v1/certificates/N/suspensions", CERTIFICATION_OFFICER),
                Arguments.of(POST, "/api/v1/certificates/N/renewals", CERTIFICATION_OFFICER),
                Arguments.of(POST, "/api/v1/certificates/N/global-renewals", CERTIFICATION_OFFICER),
                Arguments.of(POST, "/api/v1/assets/A/global-certificates", CERTIFICATION_OFFICER));
    }

    @ParameterizedTest(name = "{0} {1} is only for {2}")
    @MethodSource("writes")
    void eachWriteIsOnlyForItsRole(HttpMethod method, String uri, StaffRole allowed) throws Exception {
        for (StaffRole role : StaffRole.values()) {
            int status = mvc.perform(request(method, uri).contentType(MediaType.APPLICATION_JSON).content("{}").with(withRole(role)))
                    .andReturn().getResponse().getStatus();
            if (role == allowed) {
                assertNotEquals(403, status, role + " should be allowed");
            } else {
                assertEquals(403, status, role + " should be forbidden");
            }
        }
    }

    @Test
    void aForbiddenWriteIsAProblem() throws Exception {
        mvc.perform(request(POST, "/api/v1/assets").contentType(MediaType.APPLICATION_JSON).content("{}").with(withRole(VERIFIER)))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", "application/problem+json"));
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void everyRoleCanRead(StaffRole role) throws Exception {
        mvc.perform(get("/api/v1/assets").with(withRole(role))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void everyRoleCanLogOut(StaffRole role) throws Exception {
        mvc.perform(request(HttpMethod.DELETE, "/api/v1/tokens/current").with(withRole(role))).andExpect(status().isNoContent());
    }

    private static RequestPostProcessor withRole(StaffRole role) {
        return jwt().jwt(token -> token.subject("EMP-TEST").claim("name", "Test Person").jti("jti-" + role).expiresAt(java.time.Instant.now().plusSeconds(3600)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
