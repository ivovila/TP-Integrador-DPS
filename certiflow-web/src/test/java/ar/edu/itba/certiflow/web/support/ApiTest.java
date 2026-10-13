package ar.edu.itba.certiflow.web.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:certiflow-web;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password="})
@AutoConfigureMockMvc
public abstract class ApiTest {

    private static final String[] TABLES = {"audit_entry", "suspension", "certificate", "verification", "corrective_action",
            "finding_evidence", "finding", "revision", "observation", "attached_evidence", "criterion_response", "inspection",
            "schema_version", "inspection_schema", "asset_characteristic", "asset"};

    @Autowired
    protected MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void emptyTheDatabase() {
        for (String table : TABLES) {
            jdbcTemplate.update("delete from " + table);
        }
        jdbcTemplate.execute("alter sequence certificate_number_seq restart with 1");
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
