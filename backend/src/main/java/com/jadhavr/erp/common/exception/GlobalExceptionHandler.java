package com.jadhavr.erp.common.exception;

import com.jadhavr.erp.common.api.ErrorResponse;
import com.jadhavr.erp.common.error.ClientErrorMessage;
import com.jadhavr.erp.common.error.RequestCorrelation;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_FAILURE =
            "The request could not be completed. Please try again.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ClientErrorMessage.safe(
                        exception.getMessage(), "Resource not found")));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ClientErrorMessage.safe(
                        exception.getMessage(), "The request conflicts with existing data")));
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            RuntimeException exception, HttpServletRequest request) {
        log.warn("Concurrent update rejected correlationId={} method={} path={}",
                RequestCorrelation.getOrCreate(request), request.getMethod(),
                request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "This record was changed by another request. Reload it and try again."));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ClientErrorMessage.safe(
                exception.getMessage(), "The request is invalid")));
    }

    @ExceptionHandler(com.jadhavr.erp.admission.exception.AdmissionInformationValidationException.class)
    public ResponseEntity<ErrorResponse> handleAdmissionInformationValidation(
            com.jadhavr.erp.admission.exception.AdmissionInformationValidationException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                false,
                exception.getMessage(),
                LocalDateTime.now(),
                exception.getFieldErrors()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(
            TooManyRequestsException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER,
                        Long.toString(exception.getRetryAfterSeconds()))
                .body(new ErrorResponse(ClientErrorMessage.safe(
                        exception.getMessage(), "Too many requests"), request.getRequestURI()));
    }

    @ExceptionHandler(com.jadhavr.erp.auth.security.AuthorizationStateUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationStateUnavailable(
            com.jadhavr.erp.auth.security.AuthorizationStateUnavailableException exception,
            HttpServletRequest request) {
        return hiddenFailure(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Authorization service is temporarily unavailable",
                exception,
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        String correlationId = RequestCorrelation.getOrCreate(request);
        log.warn("Invalid argument rejected correlationId={} method={} path={}",
                correlationId, request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.badRequest()
                .header(RequestCorrelation.HEADER, correlationId)
                .body(new ErrorResponse(
                        "Request contains an invalid value", request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), ClientErrorMessage.safe(
                    fieldError.getDefaultMessage(), "This value is invalid"));
        }
        ErrorResponse response = new ErrorResponse(
                false,
                "Validation failed",
                LocalDateTime.now(),
                errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        return hiddenFailure(
                HttpStatus.CONFLICT,
                dataIntegrityMessage(exception, request),
                exception,
                request);
    }

    private String dataIntegrityMessage(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        if (!"/api/student/admissions/me/details".equals(request.getRequestURI())) {
            return "The request conflicts with existing or related data";
        }

        StringBuilder diagnostic = new StringBuilder();
        Throwable cause = exception;
        while (cause != null) {
            if (cause.getMessage() != null) {
                diagnostic.append(' ').append(cause.getMessage().toLowerCase(java.util.Locale.ROOT));
            }
            cause = cause.getCause();
        }
        String detail = diagnostic.toString();
        if (detail.contains("student_fee_accounts") && detail.contains("fee_structure_id")) {
            return "Admission fee account could not be created because the college fee setup "
                    + "is incomplete. Please contact the college office.";
        }
        return "The admission form conflicts with an existing student record. "
                + "Check the entered details and try again.";
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.warn("Unreadable request body correlationId={} method={} path={} type={}",
                RequestCorrelation.getOrCreate(request), request.getMethod(),
                request.getRequestURI(), exception.getClass().getSimpleName());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "Request body does not match the required schema, type, or format"));
    }

    @ExceptionHandler(com.jadhavr.erp.admission.service.UploadScanPendingException.class)
    public ResponseEntity<ErrorResponse> handleUploadScanPending(
            com.jadhavr.erp.admission.service.UploadScanPendingException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_EARLY)
                .header(HttpHeaders.RETRY_AFTER, "2")
                .body(new ErrorResponse(ClientErrorMessage.safe(
                        exception.getMessage(), "The uploaded file is still being checked"),
                        request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleParameterTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "Parameter '" + exception.getName() + "' has an invalid type or format"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "Required parameter '" + exception.getParameterName() + "' is missing"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestPart(
            MissingServletRequestPartException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("A required upload field is missing"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMalformedMultipart(MultipartException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("The upload request is invalid"));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBinding(BindException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Request values failed schema validation"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("The request content type is not supported"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMethod(
            HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("The request method is not supported"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingRoute(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Resource not found"));
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> handleMethodValidation(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Request parameters failed schema validation"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse(
                        "The uploaded file is too large. Passport photos must be 2 MB or smaller."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Access denied"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Unauthorized", request.getRequestURI()));
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(RuntimeException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Invalid email or password", request.getRequestURI()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("User account is inactive"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return hiddenFailure(
                HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_FAILURE, exception, request);
    }

    private ResponseEntity<ErrorResponse> hiddenFailure(
            HttpStatus status,
            String clientMessage,
            Exception exception,
            HttpServletRequest request) {
        String correlationId = RequestCorrelation.getOrCreate(request);
        log.error("Request failure hidden from client correlationId={} status={} method={} path={}",
                correlationId, status.value(), request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(status)
                .header(RequestCorrelation.HEADER, correlationId)
                .body(new ErrorResponse(
                        false,
                        clientMessage,
                        LocalDateTime.now(),
                        Map.of("correlationId", correlationId),
                        request.getRequestURI()));
    }
}
