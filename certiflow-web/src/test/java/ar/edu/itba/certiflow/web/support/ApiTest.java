package ar.edu.itba.certiflow.web.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import ar.edu.itba.certiflow.web.security.StaffRole;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = {TestDatabase.URL, TestDatabase.USER, TestDatabase.PASSWORD})
@AutoConfigureMockMvc
public abstract class ApiTest {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void emptyTheDatabase() {
        TestDatabase.empty(jdbcTemplate);
    }

    public ResultActions postAs(String personId, String uri, String json) throws Exception {
        return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json)
                .with(as(personId)));
    }

    public ResultActions putAs(String personId, String uri, String json) throws Exception {
        return mvc.perform(put(uri).contentType(MediaType.APPLICATION_JSON).content(json)
                .with(as(personId)));
    }

    public ResultActions read(String uri) throws Exception {
        return mvc.perform(get(uri).accept(MediaType.parseMediaType("application/hal+json")).with(as("EMP-READER")));
    }

    protected static RequestPostProcessor as(String personId) {
        return jwt().jwt(token -> token.subject(personId).claim("name", "Person " + personId))
                .authorities(Arrays.stream(StaffRole.values()).<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
    }
}
