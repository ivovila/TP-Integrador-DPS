package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.shared.model.DomainException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/** Arma el cuerpo de un error: el título y el tipo llevan el nombre de lo que falló. */
@Component
public class ProblemDetails {

    public ProblemDetail of(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:certiflow:" + title));
        problem.setTitle(title);
        return problem;
    }

    public ProblemDetail of(HttpStatus status, DomainException exception) {
        return of(status, exception.getClass().getSimpleName(), exception.getMessage());
    }
}
