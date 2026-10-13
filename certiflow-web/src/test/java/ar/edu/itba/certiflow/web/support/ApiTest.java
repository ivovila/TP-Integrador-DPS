package ar.edu.itba.certiflow.web.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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
                .header("X-Person-Id", personId).header("X-Person-Name", "Person " + personId));
    }

    public ResultActions putAs(String personId, String uri, String json) throws Exception {
        return mvc.perform(put(uri).contentType(MediaType.APPLICATION_JSON).content(json)
                .header("X-Person-Id", personId).header("X-Person-Name", "Person " + personId));
    }

    public ResultActions read(String uri) throws Exception {
        return mvc.perform(get(uri).accept(MediaType.parseMediaType("application/hal+json")));
    }
}
