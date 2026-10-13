package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.model.exceptions.DuplicateCharacteristicException;
import ar.edu.itba.certiflow.certification.model.SubsystemNotFoundException;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.inspection.model.exceptions.CorrectiveActionNotInFindingException;
import ar.edu.itba.certiflow.inspection.model.exceptions.CriterionOutOfScopeException;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.schema.model.AnswerTypeMismatchException;
import ar.edu.itba.certiflow.schema.model.InvalidRuleConfigurationException;
import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.NoEffectiveSchemaVersionException;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaVersionNotFoundException;
import ar.edu.itba.certiflow.schema.model.exceptions.SectionNotInSchemaException;
import ar.edu.itba.certiflow.shared.model.DomainException;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.net.URI;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Map<Class<? extends DomainException>, HttpStatus> STATUS_BY_EXCEPTION = Map.ofEntries(
            Map.entry(NoEffectiveSchemaVersionException.class, HttpStatus.NOT_FOUND),
            Map.entry(SubsystemNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(SchemaVersionNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(AssetNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(InspectionNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(FindingNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(CertificateNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(NoInspectionSchemaForAssetTypeException.class, HttpStatus.NOT_FOUND),
            Map.entry(CriterionNotInSchemaException.class, HttpStatus.NOT_FOUND),
            Map.entry(CorrectiveActionNotInFindingException.class, HttpStatus.NOT_FOUND),
            Map.entry(CriterionOutOfScopeException.class, HttpStatus.NOT_FOUND),
            Map.entry(RequiredValueMissingException.class, HttpStatus.BAD_REQUEST),
            Map.entry(InvalidSchemaException.class, HttpStatus.BAD_REQUEST),
            Map.entry(InvalidRuleConfigurationException.class, HttpStatus.BAD_REQUEST),
            Map.entry(AnswerTypeMismatchException.class, HttpStatus.BAD_REQUEST),
            Map.entry(SectionNotInSchemaException.class, HttpStatus.BAD_REQUEST),
            Map.entry(DuplicateCharacteristicException.class, HttpStatus.BAD_REQUEST),
            Map.entry(NotEligibleForCertificationException.class, HttpStatus.CONFLICT));

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handle(InvalidRequestException exception) {
        return problem(HttpStatus.BAD_REQUEST, "InvalidRequestException", exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handle(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "ResourceNotFoundException", exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ProblemDetail handle(DuplicateKeyException exception) {
        return problem(HttpStatus.CONFLICT, "DuplicateKeyException", "The resource already exists");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handle(DataIntegrityViolationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "DataIntegrityViolationException", "A value does not fit the stored format");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:certiflow:" + title));
        problem.setTitle(title);
        return problem;
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handle(DomainException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusOf(exception), exception.getMessage());
        problem.setType(URI.create("urn:certiflow:" + exception.getClass().getSimpleName()));
        problem.setTitle(exception.getClass().getSimpleName());
        if (exception instanceof NotEligibleForCertificationException notEligible) {
            problem.setProperty("ineligibilities", notEligible.decision().ineligibilities().stream()
                    .map(ineligibility -> Map.of("reason", ineligibility.reason().name(), "detail", ineligibility.detail()))
                    .toList());
        }
        return problem;
    }

    private static HttpStatus statusOf(DomainException exception) {
        for (Class<?> type = exception.getClass(); type != null; type = type.getSuperclass()) {
            HttpStatus status = STATUS_BY_EXCEPTION.get(type);
            if (status != null) {
                return status;
            }
        }
        return HttpStatus.CONFLICT;
    }
}
