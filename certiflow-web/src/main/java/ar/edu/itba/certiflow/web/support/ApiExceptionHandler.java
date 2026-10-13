package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.shared.model.InvalidValueException;
import ar.edu.itba.certiflow.shared.model.NotFoundException;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Cada excepción de dominio pertenece a una de tres categorías y la categoría decide el estado HTTP.
 * Una excepción nueva no necesita registrarse acá: alcanza con que extienda la categoría que le corresponde.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    record IneligibilityProperty(String reason, String detail) {
    }

    private final ProblemDetails problems;

    public ApiExceptionHandler(ProblemDetails problems) {
        this.problems = problems;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handle(NotFoundException exception) {
        return problems.of(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(InvalidValueException.class)
    ProblemDetail handle(InvalidValueException exception) {
        return problems.of(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(RuleViolationException.class)
    ProblemDetail handle(RuleViolationException exception) {
        return problems.of(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(NotEligibleForCertificationException.class)
    ProblemDetail handle(NotEligibleForCertificationException exception) {
        ProblemDetail problem = problems.of(HttpStatus.CONFLICT, exception);
        problem.setProperty("ineligibilities", exception.decision().ineligibilities().stream()
                .map(ineligibility -> new IneligibilityProperty(ineligibility.reason().code(), ineligibility.detail()))
                .toList());
        return problem;
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handle(InvalidRequestException exception) {
        return problems.of(HttpStatus.BAD_REQUEST, "InvalidRequestException", exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handle(ResourceNotFoundException exception) {
        return problems.of(HttpStatus.NOT_FOUND, "ResourceNotFoundException", exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ProblemDetail handle(DuplicateKeyException exception) {
        return problems.of(HttpStatus.CONFLICT, "DuplicateKeyException", "The resource already exists");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handle(DataIntegrityViolationException exception) {
        return problems.of(HttpStatus.BAD_REQUEST, "DataIntegrityViolationException",
                "A value does not fit the stored format");
    }
}
