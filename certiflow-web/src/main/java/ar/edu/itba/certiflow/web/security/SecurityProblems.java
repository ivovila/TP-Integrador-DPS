package ar.edu.itba.certiflow.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

public class SecurityProblems implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String BROWSER_REQUEST_HEADER = "X-Requested-With";
    private static final String BROWSER_REQUEST_VALUE = "XMLHttpRequest";

    private final JsonMapper jsonMapper;

    public SecurityProblems(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        if (!BROWSER_REQUEST_VALUE.equals(request.getHeader(BROWSER_REQUEST_HEADER))) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"certiflow\"");
        }
        write(request, response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Log in with Basic credentials or a valid bearer token");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "Forbidden", "Your role cannot perform this action");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String title, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), Map.of(
                "type", "urn:certiflow:" + title,
                "title", title,
                "status", status.value(),
                "detail", detail,
                "instance", request.getRequestURI()));
    }
}
