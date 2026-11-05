package it.helper730.api;

import it.helper730.domain.Msg;
import it.helper730.domain.exceptions.DomainException;
import it.helper730.engine.exceptions.UnclassifiedInstrumentsException;
import it.helper730.i18n.Messages;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * ApiExceptionHandler.
 *
 * Turns every exception into a problem detail with a stable code and a message
 * translated into the language of the request.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    /**
     * HTTP status of each domain error code. A code missing here is answered with
     * 422.
     */
    private static final Map<String, Integer> STATUS_BY_CODE = Map.of(
            "INVALID_DATA", 400,
            "UNCLASSIFIED_INSTRUMENTS", 409,
            "UNCOVERED_SALE", 422,
            "UNSUPPORTED_CURRENCY", 422,
            "UNSUPPORTED_BROKER", 422,
            "MALFORMED_CSV", 422,
            "UNSUPPORTED_TAX_YEAR", 422);

    private static final int UNPROCESSABLE = 422;

    private final Messages messages;

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException e) {
        ProblemDetail problem = problem(STATUS_BY_CODE.getOrDefault(e.code(), UNPROCESSABLE), e.code(), e.msg());
        if (e instanceof UnclassifiedInstrumentsException unclassified) {
            problem.setProperty("tickers", unclassified.tickers());
        }
        return problem;
    }

    /**
     * Body that is not JSON, or a value of the wrong type
     * (e.g. "abc" for an amount).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException e) {
        return problem(400, "MALFORMED_REQUEST", Msg.of("error.malformed_request"));
    }

    /**
     * Path or query value of the wrong type.
     * e.g. /tax/abc.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail typeMismatch(MethodArgumentTypeMismatchException e) {
        return problem(
                400, "INVALID_PARAMETER", Msg.of("error.invalid_parameter", e.getName(), String.valueOf(e.getValue())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail missingParameter(MissingServletRequestParameterException e) {
        return problem(400, "MISSING_PARAMETER", Msg.of("error.missing_parameter", e.getParameterName()));
    }

    /**
     * Upload without the "file" part.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail missingPart(MissingServletRequestPartException e) {
        return problem(400, "MISSING_PARAMETER", Msg.of("error.missing_parameter", e.getRequestPartName()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(MaxUploadSizeExceededException e) {
        return problem(413, "FILE_TOO_LARGE", Msg.of("error.file_too_large"));
    }

    /**
     * Every other exception, including Spring's own.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        if (e instanceof ErrorResponse springError) {
            int status = springError.getStatusCode().value();
            if (status >= 500) {
                log.error("Request failed", e);
            }
            return problem(status, "HTTP_" + status, Msg.of("error.http", status));
        }
        log.error("Unexpected error", e);
        return problem(500, "INTERNAL_ERROR", Msg.of("error.internal"));
    }

    private ProblemDetail problem(int status, String code, Msg msg) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), messages.text(msg));
        problem.setProperty("code", code);
        problem.setProperty("message_code", msg.code());
        problem.setProperty("params", msg.params());
        return problem;
    }
}
