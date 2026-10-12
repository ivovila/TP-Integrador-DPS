package ar.edu.itba.certiflow.web.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class ApiTest {

    @Autowired
    protected MockMvc mvc;

    protected ResultActions postAs(String personId, String uri, String json) throws Exception {
        return mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(json)
                .header("X-Person-Id", personId).header("X-Person-Name", "Person " + personId));
    }

    protected ResultActions putAs(String personId, String uri, String json) throws Exception {
        return mvc.perform(put(uri).contentType(MediaType.APPLICATION_JSON).content(json)
                .header("X-Person-Id", personId).header("X-Person-Name", "Person " + personId));
    }

    protected ResultActions read(String uri) throws Exception {
        return mvc.perform(get(uri).accept(MediaType.parseMediaType("application/hal+json")));
    }
}
