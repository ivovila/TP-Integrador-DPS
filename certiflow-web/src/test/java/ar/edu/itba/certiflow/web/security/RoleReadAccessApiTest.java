package ar.edu.itba.certiflow.web.security;

import static ar.edu.itba.certiflow.web.security.StaffRole.ASSET_RESPONSIBLE;
import static ar.edu.itba.certiflow.web.security.StaffRole.CERTIFICATION_OFFICER;
import static ar.edu.itba.certiflow.web.security.StaffRole.INSPECTION_COORDINATOR;
import static ar.edu.itba.certiflow.web.security.StaffRole.INSPECTOR;
import static ar.edu.itba.certiflow.web.security.StaffRole.VERIFIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import ar.edu.itba.certiflow.web.support.ApiTest;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class RoleReadAccessApiTest extends ApiTest {

    private static final Set<StaffRole> EVERYONE = EnumSet.allOf(StaffRole.class);
    private static final Set<StaffRole> INSPECTION_READERS = EnumSet.of(INSPECTION_COORDINATOR, INSPECTOR, CERTIFICATION_OFFICER);
    private static final Set<StaffRole> FINDING_READERS = EnumSet.of(ASSET_RESPONSIBLE, VERIFIER);
    private static final Set<StaffRole> CERTIFICATE_READERS = EnumSet.of(CERTIFICATION_OFFICER);

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    static Stream<Arguments> reads() {
        return Stream.of(
                Arguments.of("/api/v1", EVERYONE),
                Arguments.of("/api/v1/vocabulary", EVERYONE),
                Arguments.of("/api/v1/assets", EVERYONE),
                Arguments.of("/api/v1/assets/A", EVERYONE),
                Arguments.of("/api/v1/schemas", EVERYONE),
                Arguments.of("/api/v1/schemas/T", EVERYONE),
                Arguments.of("/api/v1/inspections", INSPECTION_READERS),
                Arguments.of("/api/v1/inspections/I", INSPECTION_READERS),
                Arguments.of("/api/v1/inspections/I/act", INSPECTION_READERS),
                Arguments.of("/api/v1/inspections/I/certification-eligibility", INSPECTION_READERS),
                Arguments.of("/api/v1/findings", FINDING_READERS),
                Arguments.of("/api/v1/findings/F", FINDING_READERS),
                Arguments.of("/api/v1/assets/A/findings-summary", FINDING_READERS),
                Arguments.of("/api/v1/certificates", CERTIFICATE_READERS),
                Arguments.of("/api/v1/certificates/N", CERTIFICATE_READERS),
                Arguments.of("/api/v1/certificates/N/document", CERTIFICATE_READERS));
    }

    @ParameterizedTest(name = "GET {0} is for {1}")
    @MethodSource("reads")
    void eachCollectionIsReadOnlyByTheRolesThatWorkOnIt(String uri, Set<StaffRole> readers) throws Exception {
        for (StaffRole role : StaffRole.values()) {
            int status = mvc.perform(get(uri).with(withRole(role))).andReturn().getResponse().getStatus();
            if (readers.contains(role)) {
                assertNotEquals(403, status, role + " should read " + uri);
            } else {
                assertEquals(403, status, role + " should not read " + uri);
            }
        }
    }

    @Test
    void everyControllerThatAnswersReadsDeclaresWhoReadsIt() {
        List<String> undeclared = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getKey().getMethodsCondition().getMethods().contains(RequestMethod.GET))
                .map(entry -> entry.getValue().getBeanType())
                .filter(type -> type.getPackageName().startsWith("ar.edu.itba.certiflow"))
                .filter(type -> !type.isAnnotationPresent(ReadableBy.class) && !type.isAnnotationPresent(ReadableByAnyone.class))
                .map(Class::getSimpleName)
                .distinct()
                .toList();

        assertEquals(List.of(), undeclared);
    }

    private static RequestPostProcessor withRole(StaffRole role) {
        return jwt().jwt(token -> token.subject("EMP-TEST").claim("name", "Test Person"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
