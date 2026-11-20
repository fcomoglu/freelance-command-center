package com.freelancecommandcenter.shared.web;

import com.freelancecommandcenter.shared.error.DomainException;
import com.freelancecommandcenter.shared.error.ErrorKind;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into RFC 9457 Problem Details with a stable {@code code} property that clients translate (see
 * docs/architecture.md, API conventions).
 */
@RestControllerAdvice
class ProblemDetailsHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsHandler.class);

    private static final Map<ErrorKind, HttpStatus> STATUS = Map.of(
            ErrorKind.INVALID, HttpStatus.BAD_REQUEST,
            ErrorKind.UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
            ErrorKind.FORBIDDEN, HttpStatus.FORBIDDEN,
            ErrorKind.NOT_FOUND, HttpStatus.NOT_FOUND,
            ErrorKind.CONFLICT, HttpStatus.CONFLICT,
            ErrorKind.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS,
            ErrorKind.UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handleDomain(DomainException exception) {
        HttpStatus status = STATUS.get(exception.kind());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setProperty("code", exception.code());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setProperty("code", "server.error");
        return ResponseEntity.internalServerError().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        problem.setProperty("code", "validation.failed");
        problem.setProperty(
                "errors",
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> new FieldErrorItem(error.getField(), validationCode(error.getCode())))
                        .toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    /** Constraint violations on query and path parameters, reported in the same shape as body validation errors. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setProperty("code", "validation.failed");
        problem.setProperty(
                "errors",
                exception.getParameterValidationResults().stream()
                        .flatMap(result -> result.getResolvableErrors().stream()
                                .map(error -> new FieldErrorItem(
                                        result.getMethodParameter().getParameterName(),
                                        validationCode(constraintName(error.getCodes())))))
                        .toList());
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Adds a generic code to the Problem Details Spring creates for framework errors (malformed JSON, 404, ...). */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey("code"))) {
            problem.setProperty(
                    "code",
                    "request."
                            + snakeCase(HttpStatus.valueOf(statusCode.value()).name()));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static String validationCode(String constraint) {
        return "validation." + snakeCase(constraint == null ? "invalid" : constraint);
    }

    /** The least specific message code is the constraint name ({@code Max}). */
    private static String constraintName(String[] codes) {
        return codes == null || codes.length == 0 ? null : List.of(codes).getLast();
    }

    /** {@code NotBlank} becomes {@code not_blank}; {@code BAD_REQUEST} becomes {@code bad_request}. */
    private static String snakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    record FieldErrorItem(String field, String code) {}
}
